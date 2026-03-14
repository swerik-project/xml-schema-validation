# riksdagen-validation

Validate ParlaClarin TEI XML files with Scala / Java-XML

Usage example

```
mill run parla-clarin.xsd ../riksdagen-records/data/1971/prot-1971--00*.xml test.xml Parla-CLARIN-Exemplar.xml
```

Some environments might require running mill with `./mill`.


Dependencies

- Scala 2.13.11
- [mill build tool](https://mill-build.com/mill/Intro_to_Mill.html) for Scala
- Some Scala modules defined in build.sc
