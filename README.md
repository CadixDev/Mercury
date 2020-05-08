Mercury
=======

Mercury is a source code transformation library for Java codebases, with an
included remapper - built on [Eclipse's JDT] and [Lorenz]. Mercury is licensed
under the [Eclipse Public License 2.0](LICENSE).

While Mercury is yet to see a proper release (we're working on it), it has
been successfully used by the following projects:

- **[The Fabric Project]** uses Mercury in their
  [Gradle build tools](https://fabricmc.net/wiki/tutorial:migratemappings) to
  remap mods to their latest yarn mappings.
- **[The Sponge Project]** uses Mercury for their cross-version mapping
  migration tool, saving hundreds of man-hours of work - and with greater
  accuracy.

## Usage

Mercury is centred around the `Mercury` class, which is the effective control
centre. *See the following example, for a demonstration of remapping a codebase
using Lorenz*.

```java
final Mercury mercury = new Mercury();

// Due to the nature of how mercury works, it is essential that we have
// the full binary classpath.
mercury.getClassPath().add(Paths.get("example.jar"));

// To remap the codebase using Lorenz, we must install a MercuryRemapper,
// with our MappingSet.
mercury.getProcessors().add(MercuryRemapper.create(mappings));

// Lets rewrite our codebase at "a/" to "b/".
mercury.rewrite(Paths.get("a"), Paths.get("b"));
```

### Rewrite access transformers

Mercury also has an optional dependency on [at], allowing access transformers
to be applied to source, and be updated to reflect changes in the structure of
the codebase. *See the following example to see how this could work*.

```java
final Mercury mercury = new Mercury();
mercury.getClassPath().add(Paths.get("example.jar"));
mercury.getProcessors().add(MercuryRemapper.create(mappings));

// Since we run after MercuryRemapper, the access transformer needs to be
// remapped - fortunately, at provides an optional dependency on Lorenz,
// and a utility to do just that!
final AccessTransformSet remappedTransforms =
            AccessTransformSetMapper.remap(transforms, mappings);
// We can then use AccessTransformerRewriter with the remapped access
// transform set.
mercury.getProcessors()
            .add(AccessTransformerRewriter.create(remappedTransforms));

mercury.rewrite(Paths.get("a"), Paths.get("b"));
```

## Discuss

**Found an issue with Mercury?** [Make an issue]! We'd rather close invalid
reports than have bugs go unreported :)

We have an IRC channel on [EsperNet], `#cadix`, which is available for all
[registered](https://esper.net/getting_started.php#registration) users to join
and discuss Mercury and other Cadix projects.

[Eclipse's JDT]: https://www.eclipse.org/jdt/
[Lorenz]: https://github.com/CadixDev/Lorenz
[The Fabric Project]: https://fabricmc.net/
[The Sponge Project]: https://www.spongepowered.org/
[at]: https://github.com/CadixDev/at
[Make an issue]: https://github.com/CadixDev/Mercury/issues/new
[EsperNet]: https://esper.net/
