import numpy as np, wave, sys
SR = 44100
rng = np.random.default_rng(10)

def env(n, attack_ms, decay_ms):
    t = np.arange(n)/SR
    a = np.clip(t/(attack_ms/1000), 0, 1)
    return a*a*(3-2*a) * np.exp(-t/(decay_ms/1000))

def biquad(x, f, q, kind):
    f = min(f, SR*0.45)
    w0 = 2*np.pi*f/SR; al = np.sin(w0)/(2*q); c = np.cos(w0)
    if kind == 'lp':   b0, b1, b2 = (1-c)/2, 1-c, (1-c)/2
    elif kind == 'hp': b0, b1, b2 = (1+c)/2, -(1+c), (1+c)/2
    else:              b0, b1, b2 = al, 0.0, -al
    a0, a1, a2 = 1+al, -2*c, 1-al
    b0, b1, b2, a1, a2 = b0/a0, b1/a0, b2/a0, a1/a0, a2/a0
    y = np.zeros_like(x); x1=x2=y1=y2=0.0
    for i, xi in enumerate(x):
        yi = b0*xi + b1*x1 + b2*x2 - a1*y1 - a2*y2
        x2, x1, y2, y1 = x1, xi, y1, yi; y[i] = yi
    return y

def one_hit(f, ms, decay, *, partials, bright=1.0, glide=0.0, noise=0.4, noise_f=1800, tail=0.0, tail_decay=80):
    """ONE event: a single excitation into harmonic partials. Everything starts at t=0,
    so it is always heard as one hit. glide bends pitch (semitones) over the first 30 ms:
    up = 'engage', down = 'release'. tail adds a soft sustained ring (for 'locked')."""
    n = int(SR*ms/1000); t = np.arange(n)/SR
    bend = 2**((glide/12) * (1 - np.exp(-t/0.012)))           # quick, then holds
    phase = 2*np.pi*np.cumsum(f*bend)/SR
    s = np.zeros(n)
    for k, amp, dmul in partials:
        s += amp * np.sin(k*phase) * env(n, 0.6, decay*dmul)
    if tail:
        s += tail * (np.sin(phase) + 0.35*np.sin(2*phase)) * env(n, 2.0, tail_decay)
    burst = biquad(rng.standard_normal(n), noise_f, 0.9, 'bp') * env(n, 0.05, 1.2)   # the contact, same instant
    s = s + noise*burst
    s = biquad(s, 900 + 2600*bright, 0.7, 'lp')
    return biquad(s, 90, 0.7, 'hp')

def felt_loudness(s):
    """Loudness as ears hear it on a small speaker: cut lows, lift presence, RMS over the hit."""
    x = biquad(s, 250, 0.7, 'hp')
    x = x + 0.6*biquad(x, 2500, 0.7, 'hp')
    w = x[:int(SR*0.08)]
    return 20*np.log10(np.sqrt(np.mean(w**2)) + 1e-9)

def finish(s, target):
    fade = int(SR*0.010); s[-fade:] *= np.linspace(1, 0, fade)**2
    s -= np.mean(s)
    s *= 10**((target - felt_loudness(s))/20)
    peak = np.max(np.abs(s)); lim = 10**(-1.0/20)
    if peak > lim: s *= lim/peak
    return s

def save(name, s):
    with wave.open(name, 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((np.clip(s,-1,1)*32767).astype('<i2').tobytes())

# One loudness hierarchy for every pack (felt dB). Triggers get a little more weight.
LEVEL = {'move': -24, 'type': -16, 'space': -16, 'delete': -17, 'on': -14.5, 'off': -15.5,
         'lock': -13.5, 'select_all': -15, 'confirm': -15.5, 'back': -18}

# ---------------------------------------------------------------- STUDIO: felt thocks
C3, D3, G3 = 130.81, 146.83, 196.00
TH = [(1, 1.0, 1.0), (2, 0.9, 0.8), (3, 0.5, 0.55), (4, 0.22, 0.4)]     # low note carried by overtones
LATCH = [(1, 1.0, 1.0), (2, 1.0, 0.9), (3, 0.6, 0.7), (4, 0.3, 0.5), (6, 0.12, 0.3)]
studio = {
    'move':   one_hit(G3*2, 35, 7, partials=TH, bright=0.3, noise=0.25, noise_f=1600),
    'type':   one_hit(G3, 70, 16, partials=TH, bright=0.55),
    'space':  one_hit(D3, 80, 20, partials=TH, bright=0.5),
    'delete': one_hit(C3, 65, 13, partials=TH, bright=0.35, noise=0.3),
    'on':     one_hit(D3, 110, 26, partials=LATCH, bright=0.6, glide=+2.0, noise=0.5),      # heavier "latch", bends up
    'off':    one_hit(D3, 100, 22, partials=LATCH, bright=0.4, glide=-2.0, noise=0.4),      # same latch, bends down
    'lock':   one_hit(C3, 200, 30, partials=LATCH, bright=0.55, glide=+2.0, noise=0.55, tail=0.35, tail_decay=90),
    'select_all': one_hit(G3, 170, 22, partials=TH, bright=0.7, glide=+5.0, noise=0.4, tail=0.2, tail_decay=60),
    'confirm':    one_hit(G3, 200, 30, partials=[(1,1,1),(1.5,0.55,0.9),(2,0.6,0.7),(3,0.3,0.5)], bright=0.55, noise=0.3, tail=0.25, tail_decay=90),
    'back':       one_hit(D3, 150, 24, partials=TH, bright=0.3, glide=-1.0, noise=0.25),
}

# ---------------------------------------------------------------- TACTILE: dry clicks
def click(tone, ms, decay, *, body=0.35, bright=1.0, glide=0.0, tail=0.0):
    """A switch: mostly the contact noise, a small unpitched-ish body underneath."""
    return one_hit(tone, ms, decay, partials=[(1, body, 1.0), (2.3, body*0.5, 0.6)],
                   bright=bright, glide=glide, noise=1.0, noise_f=tone*6, tail=tail*body, tail_decay=50)
tactile = {
    'move':   click(420, 25, 4, body=0.15, bright=0.5),
    'type':   click(330, 45, 7),
    'space':  click(260, 55, 9, body=0.45, bright=0.8),
    'delete': click(230, 50, 7, body=0.4, bright=0.6),
    'on':     click(250, 90, 16, body=0.8, bright=0.8, glide=+2.0),
    'off':    click(250, 80, 13, body=0.7, bright=0.6, glide=-2.0),
    'lock':   click(200, 150, 22, body=1.0, bright=0.8, glide=+2.0, tail=0.5),
    'select_all': click(300, 110, 14, body=0.6, bright=1.0, glide=+5.0),
    'confirm':    click(280, 110, 16, body=0.7, bright=0.8),
    'back':       click(220, 90, 12, body=0.5, bright=0.5, glide=-1.0),
}

# ---------------------------------------------------------------- CHIME: soft mallet notes
C5 = 523.25; R = {'C':1, 'D':9/8, 'E':5/4, 'G':3/2, 'A':5/3}
def nt(n, o): return C5*R[n]*2**(o-5)
MAL = [(1, 1.0, 1.0), (2, 0.22, 0.45), (4, 0.10, 0.18)]
def chord(notes, ms, decay, bright=0.8):
    """Several notes struck at the SAME instant: one event, never a sequence."""
    return sum(one_hit(f, ms, decay, partials=MAL, bright=bright, noise=0.25, noise_f=5000) for f in notes)
chime = {
    'move':   one_hit(nt('C',7), 40, 9, partials=MAL, bright=0.6, noise=0.15, noise_f=6000),
    'type':   one_hit(nt('G',6), 70, 16, partials=MAL, noise=0.3, noise_f=6000),
    'space':  one_hit(nt('C',6), 80, 18, partials=MAL, noise=0.3, noise_f=5000),
    'delete': one_hit(nt('G',5), 80, 16, partials=MAL, bright=0.6, noise=0.25, noise_f=4000),
    'on':     chord([nt('C',6), nt('G',6)], 150, 28),
    'off':    chord([nt('C',6), nt('G',5)], 140, 24, bright=0.6),
    'lock':   chord([nt('C',6), nt('E',6), nt('G',6)], 240, 40),
    'select_all': chord([nt('C',6), nt('G',6), nt('C',7)], 220, 34),
    'confirm':    chord([nt('G',5), nt('C',6)], 260, 60, bright=0.7),
    'back':       chord([nt('C',6), nt('G',5)], 220, 45, bright=0.5),
}

for prefix, pack in (('studio', studio), ('tactile', tactile), ('chime', chime)):
    for key, s in pack.items():
        save(f'{prefix}_{key}.wav', finish(s, LEVEL[key]))
print('ok')
