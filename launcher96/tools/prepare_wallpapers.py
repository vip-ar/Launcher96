"""يحوّل أي صور إلى 1920x1080 بصيغة WebP ويضعها في assets/wallpapers.

الاستخدام:
    pip install pillow
    python tools/prepare_wallpapers.py مجلد_الصور_الأصلية
"""
import pathlib
import sys

from PIL import Image, ImageOps

if len(sys.argv) < 2:
    sys.exit("حط مسار مجلد الصور الأصلية بعد اسم السكربت")

src = pathlib.Path(sys.argv[1])
dst = pathlib.Path(__file__).resolve().parent.parent / "app/src/main/assets/wallpapers"
dst.mkdir(parents=True, exist_ok=True)

exts = {".jpg", ".jpeg", ".png", ".webp"}
files = sorted(p for p in src.iterdir() if p.suffix.lower() in exts)
if not files:
    sys.exit("ما لقيت صور في المجلد")

for i, p in enumerate(files, 1):
    im = ImageOps.exif_transpose(Image.open(p)).convert("RGB")
    im = ImageOps.fit(im, (1920, 1080), Image.LANCZOS, centering=(0.5, 0.5))
    out = dst / f"wall_{i:02d}.webp"
    im.save(out, "WEBP", quality=82, method=6)
    print(out.name, out.stat().st_size // 1024, "KB")
