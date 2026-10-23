# Curios port release recognition — 0.5.2

The Curios fixes are implemented in the owning ports:

- ProjectE Reforged `26.1.2-1.1.2` supplies its two existing empty-slot textures
  through the GUI atlas used by Curios 15.
- Industrial Foregoing `26.1.2-3.6.39-viking602.3` removes its stray `example`
  entity-slot assignment.

Compat 0.5.2 contains no Curios texture or slot override. It updates the filename
used by 22 existing Industrial Foregoing source guards. Every guarded source
entry was verified byte-identical between the old and new port JARs, and the
expected SHA-256 values are unchanged. This preserves the existing model, fluid
texture and manual repairs after the port filename changes.

The Ice and Fire EMC mixin also accepts ProjectE `26.1.2-1.1.2` in addition to
`26.1.2-1.1.1`. All ProjectE runtime classes are byte-identical between these
releases; the exact Ice and Fire version requirement remains unchanged.

The production JAR differs from 0.5.1 only in version metadata, this existing
mixin gate and the source-guard manifest. Existing compatibility, production and
material guard suites passed 345, 417 and 36 assertions respectively.

The joint full-pack client check passed on 2026-10-17: all 442 existing resource
repairs remained active, no source guard was skipped, and the Ice and Fire EMC
gate remained enabled. The real Curios inventory had fourteen slot types,
sixteen visible slots and zero missing icons. Both port JARs and Compat were
installed only after their tested hashes were verified. The prior three JARs
were backed up; the other 224 mod JARs and 117 protected game files retained
their original hashes.

Local joint runtime and installation evidence is kept in
`.work/verification/curios-slots-20261017` in the containing instance workspace.
