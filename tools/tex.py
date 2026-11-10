import random, math, sys
from PIL import Image, ImageDraw, ImageFont
A = sys.argv[1] + "/assets/onepiece_newworld/textures"
LOGO = sys.argv[1] + "/logo.png"
def img(n=16): return Image.new("RGBA",(n,n),(0,0,0,0))
def hexc(h,a=255): return ((h>>16)&255,(h>>8)&255,h&255,a)
def shade(c,f): return tuple(max(0,min(255,int(v*f))) for v in c[:3])+(c[3],)

def fruit(name, base, accent, leaf=0x3E8E3A, pattern="lattice"):
    im=img(); px=im.load(); cx,cy,r=7.5,9.0,6.2; b=hexc(base); a=hexc(accent)
    for y in range(16):
        for x in range(16):
            d=math.hypot(x-cx,(y-cy)*1.08)
            if d<=r:
                if pattern=="lattice": mark=(x+y)%4==0 or (x-y)%4==0
                elif pattern=="bands": mark=(y%3==0)
                else: mark=((x//2+y//2)%2==0)
                c=a if mark else b
                lf=1.15-0.09*((x-cx)+(y-cy))/2
                if d>r-1: lf*=0.7
                px[x,y]=shade(c,lf)
    out=im.copy(); po=out.load()
    for y in range(16):
        for x in range(16):
            if px[x,y][3]==0 and any(0<=x+dx<16 and 0<=y+dy<16 and px[x+dx,y+dy][3] for dx,dy in((1,0),(-1,0),(0,1),(0,-1))):
                po[x,y]=shade(b,0.35)
    im=out; px=im.load()
    px[5,6]=(255,255,255,220); px[4,7]=(255,255,255,150)
    st=hexc(0x5A3A1E); px[8,1]=st; px[8,2]=st; px[7,2]=st
    lc=hexc(leaf)
    for p in [(9,1),(10,1),(11,2),(10,2),(9,2)]: px[p]=lc
    px[11,1]=shade(lc,1.25)
    im.save(f"{A}/item/{name}.png")

fruit("gomu_fruit",0x8A4FD6,0xC7A0FF,pattern="lattice")
fruit("mera_fruit",0xE0521B,0xFFC23A,pattern="bands")
fruit("hie_fruit",0x6EC6E8,0xEFFBFF,leaf=0x5BA88A,pattern="checker")

im=img(); d=ImageDraw.Draw(im)
d.polygon([(8,1),(12,6),(10,14),(5,13),(3,6)],fill=hexc(0x3F7E9C),outline=hexc(0x1D3E4F))
d.line([(8,2),(7,12)],fill=hexc(0x7CC4E0)); d.line([(8,2),(11,7)],fill=hexc(0x9ED8EE)); im.putpixel((6,5),(230,250,255,255))
im.save(f"{A}/item/kairoseki_shard.png")

im=img(); d=ImageDraw.Draw(im)
for i in range(10): d.point((3+i,12-i),fill=hexc(0x4C8EAA) if i>3 else hexc(0x5A3A1E))
for i in range(9): d.point((4+i,12-i),fill=hexc(0x2E5D72) if i>3 else hexc(0x3E2814))
d.line([(6,7),(9,10)],fill=hexc(0x8AA0AA)); d.line([(9,7),(10,6)],fill=hexc(0x8AA0AA)); d.point((11,5),fill=hexc(0x8AA0AA))
d.point((13,2),fill=hexc(0xBFE6F4)); d.point((2,13),fill=hexc(0xC9A24A)); d.point((3,13),fill=hexc(0xC9A24A))
im.save(f"{A}/item/kairoseki_jitte.png")

im=img(); d=ImageDraw.Draw(im)
d.ellipse((1,6,7,12),outline=hexc(0x3F7E9C),width=2); d.ellipse((9,6,15,12),outline=hexc(0x3F7E9C),width=2)
d.point((3,7),fill=hexc(0x9ED8EE)); d.point((11,7),fill=hexc(0x9ED8EE))
for x in (7,8,9): d.point((x,8),fill=hexc(0x8A8A8A))
im.save(f"{A}/item/kairoseki_cuffs.png")

def ore(name,stone,seed):
    random.seed(seed); im=img(); px=im.load()
    for y in range(16):
        for x in range(16): px[x,y]=shade(hexc(stone),random.uniform(0.82,1.12))
    for (cx,cy) in [(3,3),(10,4),(5,10),(12,11),(8,7)]:
        for dx,dy in [(0,0),(1,0),(0,1),(1,1),(-1,0)]:
            x,y=cx+dx,cy+dy
            if 0<=x<16 and 0<=y<16: px[x,y]=shade(hexc(0x3A8DB0),random.uniform(0.8,1.15))
        px[cx,cy]=hexc(0xA8E6F5)
    im.save(f"{A}/block/{name}.png")
ore("kairoseki_ore",0x7F7F7F,1); ore("deepslate_kairoseki_ore",0x4A4A4F,2)
random.seed(3); im=img(); px=im.load()
for y in range(16):
    for x in range(16):
        edge=x in(0,15) or y in(0,15); f=random.uniform(0.88,1.08)*(0.7 if edge else 1)
        if (x+y)%7==0 and not edge: f*=1.25
        px[x,y]=shade(hexc(0x2F6D8A),f)
for p in [(4,4),(11,5),(6,11),(12,12)]: px[p]=hexc(0xBFE9F7)
im.save(f"{A}/block/kairoseki_block.png")

im=Image.new("RGBA",(18,18),(0,0,0,0)); d=ImageDraw.Draw(im)
d.polygon([(9,1),(15,9),(9,17),(3,9)],fill=hexc(0x3F7E9C),outline=hexc(0x1D3E4F)); d.line([(9,2),(9,15)],fill=hexc(0x7CC4E0))
im.save(f"{A}/mob_effect/seastone.png")

# ---------- скины NPC (раскладка скина игрока 64x64) ----------
def box(px,u,v,w,h,dp,face):
    """Заполнить развёртку куба: face(name, x, y, fw, fh) -> цвет."""
    faces={"top":(u+dp,v,w,dp),"bottom":(u+dp+w,v,w,dp),"right":(u,v+dp,dp,h),"front":(u+dp,v+dp,w,h),
           "left":(u+dp+w,v+dp,dp,h),"back":(u+dp+w+dp,v+dp,w,h)}
    for name,(fx,fy,fw,fh) in faces.items():
        for y in range(fh):
            for x in range(fw):
                c=face(name,x,y,fw,fh)
                if c is not None: px[fx+x,fy+y]=c

def skin(path, skin_c, hair_c, hat, shirt, shirt2, stripes, pants, boots, belt, seed, collar=None, eye=0x2A1E14):
    random.seed(seed); im=Image.new("RGBA",(64,64),(0,0,0,0)); px=im.load()
    def n(c): return shade(c,random.uniform(0.93,1.06))
    S=hexc(skin_c); H=hexc(hair_c)
    def head(name,x,y,w,h):
        if name=="top": return n(H)
        if name=="bottom": return n(S)
        if y<2 and name!="front": return n(H)
        if name=="back": return n(H) if y<6 else n(S)
        if name=="front":
            if y<2: return n(H)
            if y==4 and x in(1,2): return hexc(0xFFFFFF) if x==1 else hexc(eye)
            if y==4 and x in(5,6): return hexc(eye) if x==5 else hexc(0xFFFFFF)
            if y==6 and 2<=x<=5: return shade(S,0.75)
        return n(S)
    box(px,0,0,8,8,8,head)
    hatc=hexc(hat[1])
    def hatf(name,x,y,w,h):
        if hat[0]=="bandana":
            if name=="top": return n(hatc)
            if y<=2: return n(hatc) if not (name=="front" and (x+y)%3==0) else hexc(0xFFFFFF)
            if name=="back" and y<=5 and 3<=x<=4: return n(hatc)
            return None
        if hat[0]=="cap":
            if name=="top": return n(hatc)
            if y<=2: return n(hatc) if y<2 else hexc(hat[2])
            return None
    box(px,32,0,8,8,8,hatf)
    SH=hexc(shirt); SH2=hexc(shirt2); P=hexc(pants); BO=hexc(boots); BE=hexc(belt)
    def torso(name,x,y,w,h):
        if name in("top","bottom"): return n(SH)
        if y>=10: return n(BE) if not(name=="front" and 3<=x<=4) else hexc(0xC9A24A)
        if collar and y<=1 and name in("front","back"): return hexc(collar)
        if collar and name=="front" and y<=3 and 2<=x<=5: return hexc(collar) if (x in(2,5) or y<=1) else n(S)
        if stripes: return n(SH2) if y%3==0 else n(SH)
        return n(SH)
    box(px,16,16,8,12,4,torso)
    def arm(name,x,y,w,h):
        if name=="top": return n(SH)
        if name=="bottom": return n(S)
        if y>=9: return n(S)
        if stripes: return n(SH2) if y%3==0 else n(SH)
        return n(SH)
    box(px,40,16,4,12,4,arm); box(px,32,48,4,12,4,arm)
    def leg(name,x,y,w,h):
        if name=="top": return n(P)
        if name=="bottom" or y>=9: return n(BO)
        return n(P)
    box(px,0,16,4,12,4,leg); box(px,16,48,4,12,4,leg)
    im.save(path)

skin(f"{A}/entity/pirate_grunt.png", 0xC8956A, 0x3B2414, ("bandana",0xB8322A), 0xEDE6D6, 0x2B3A67, True,
     0x4A3B2A, 0x2A1C12, 0x5A3A1E, 4)
skin(f"{A}/entity/marine_recruit.png", 0xE2B58C, 0x6B4A2B, ("cap",0xF4F4F4,0x2B5BA8), 0xF4F4F4, 0xF4F4F4, False,
     0xEDEDED, 0x1E1E24, 0x2B5BA8, 5, collar=0x2B5BA8)

W,H=400,160; im=Image.new("RGBA",(W,H)); d=ImageDraw.Draw(im)
for y in range(H): d.line([(0,y),(W,y)],fill=shade(hexc(0x13466A),0.55+0.6*y/H))
for k in range(4):
    yb=H-30+k*8; d.line([(x,yb+4*math.sin(x/14+k)) for x in range(0,W,2)],fill=hexc(0xBFE9F7,180-k*30),width=2)
try: f=ImageFont.truetype("DejaVuSans-Bold.ttf",40); s=ImageFont.truetype("DejaVuSans.ttf",16)
except Exception: f=s=ImageFont.load_default()
d.text((W//2,58),"NEW WORLD",font=f,fill=hexc(0xFFC94A),anchor="mm",stroke_width=3,stroke_fill=hexc(0x2A1A08))
d.text((W//2,98),"fruits · haki · bounties",font=s,fill=hexc(0xEFFBFF),anchor="mm")
im.save(LOGO)
print("ok")
