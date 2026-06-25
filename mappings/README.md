# Local mappings

Minecraft 26.2 is distributed unobfuscated, but Architectury Loom still expects a mapping artifact for Fabric and Quilt projects. `official-mappings.jar` contains an identity Tiny v2 mapping from `official` to `named` so those projects can compile without a published Yarn or Mojang mapping artifact.

Forge and NeoForge use `dev.architectury.loom-no-remap` and do not consume this mapping jar.