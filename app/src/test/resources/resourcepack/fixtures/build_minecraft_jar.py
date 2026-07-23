import zipfile, pathlib

OUT = pathlib.Path(__file__).with_name("minecraft.jar")

def main():
    with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as zf:
        wanted = {
            "assets/minecraft/models/block/stone.json": b"{}",
            "assets/minecraft/blockstates/stone.json": b"{}",
            "assets/minecraft/textures/block/stone.png": b"\x89PNG\r\n\x1a\n",
            "assets/minecraft/lang/en_us.json": b"{}",
        }
        noise = {
            "META-INF/MANIFEST.MF": b"Manifest-Version: 1.0",
            "assets/minecraft/sounds/foo.ogg": b"noise",
            "assets/create/models/block/shaft.json": b"create-only",
        }
        for path, data in {**wanted, **noise}.items():
            zf.writestr(path, data)

if __name__ == "__main__":
    main()
    print(f"wrote {OUT} ({OUT.stat().st_size} bytes)")
