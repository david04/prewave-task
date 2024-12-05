# Preware Task (David Antunes)

There are two versions: [AlertProcessorCatsEffect.scala](src/main/scala/scratchpad/AlertProcessorCatsEffect.scala), and [AlertProcessorFS2.scala](src/main/scala/scratchpad/AlertProcessorFS2.scala). One is based on Cats Effect only, the second uses FS2 on top.

The one with FS2 on top streams the results to file as they are matched.

### Configuration

Please add a file 'application.conf' to resources folder with a single line:
```
key="david:XXXXX"
```

### Running

 - Cats effect version:
```bash
sbt "runMain scratchpad.AlertProcessorCatsEffect"
```

 - FS2 version:
```bash
sbt "runMain scratchpad.AlertProcessorFS2"
```

### Output

Output is saved in a file `output.txt` in CSV style, with `alertId,queryTermId`.
