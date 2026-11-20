"""Compare the actual package payload while allowing the APK signature to differ."""

import re
import sys
from zipfile import ZipFile

v1_signature = re.compile(r"META-INF/(?:MANIFEST\.MF|[^/]+\.(?:SF|RSA|EC|DSA))$", re.IGNORECASE)


def payload(archive):
    return {name for name in archive.namelist() if not name.endswith("/") and not v1_signature.fullmatch(name)}


with ZipFile(sys.argv[1]) as rebuilt, ZipFile(sys.argv[2]) as signed:
    expected = payload(rebuilt)
    actual = payload(signed)
    if expected != actual:
        sys.exit(f"APK payload entries differ: {sorted(expected ^ actual)}")
    for name in sorted(expected):
        if rebuilt.read(name) != signed.read(name):
            sys.exit(f"APK payload differs at {name}")
    print(f"Verified {len(expected)} payload entries against the tag build.")
