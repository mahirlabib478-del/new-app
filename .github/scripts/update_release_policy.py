import json
import os
from pathlib import Path

path = Path("update.json")
if path.exists():
    policy = json.loads(path.read_text())
    tag = os.environ.get("RELEASE_TAG", "")
    repo = os.environ.get("REPOSITORY", "")
    version = tag.removeprefix("v")

    policy["latestVersion"] = version
    policy["releaseUrl"] = f"https://github.com/{repo}/releases/tag/{tag}"
    policy["apkUrl"] = f"https://github.com/{repo}/releases/download/{tag}/app-release.apk"

    path.write_text(json.dumps(policy, indent=2) + "\n")
    print(f"Updated update.json for {tag}")
else:
    print("update.json not found, skipping update.")
