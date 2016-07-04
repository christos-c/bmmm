# BMMM
The Bayesian Multinomial Mixture Model code from my 2011 paper (and thesis)

#### Requirements
1. Java 1.7
2. Maven (http://maven.apache.org/download.cgi)

#### Running BMMM

After cloning the project, or downloading the zip, open the bmmm folder in command line and run:
```
mvn package
mvn dependency:copy-dependencies
```

If the build is successful, to see the available runtime configuration options run
```
java -cp target/bmmm-2.0.11.jar:target/dependency/* tagInducer.Inducer
```

The main requirement is a CoNLL-style file with UPOS annotation (9 columns in total) as input. If the the input file 
contains dependencies (column 8) the `deps` feature can also be used. To use morphology (Morfessor) and PARG-based features
you will need the appropriate files. You can convert a raw tokenised corpus to CoNLL format using the following command:
```
java -cp target/bmmm-2.0.10.jar tagInducer.utils.RawToCoNLL corpus.txt
```

You can also use a JSON file format with the following fields (one sentence per line):
```
{
    "words":[{"word":"more","pos":"qn","upos":"DET","cluster":"48"},
        {"word":"juice","pos":"n","upos":"NOUN","cluster":"48"},
        {"word":"?","pos":"?","upos":".","cluster":"-1"}]
}
```