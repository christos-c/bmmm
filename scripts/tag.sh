#!/bin/bash
### The tagging component
## Will receive raw text (language.raw) augmentent with either:
## a)morphology (language.seg)
## b)dependency (language.dep)
## c)alignments (src-trg.aligns)
## Will output tagged file (language.tagged_i) for each iteration i

#Folder containing both $src.raw and $trg.raw
folder=;language=;out_file=
morph_file=;deps_file=;aligns_file=;aligns_trg=
align_params=;morph_params=;dep_params=

function usage {
	echo "
	usage: $0 options

	Runs the BMMM tagger on a raw corpus with optional features.

	OPTIONS:
	   -h	Show this message
	   -f	Folder containing raw file(s)
	   -o	Output file (with iteration number)
	   -l	Language name (lowercase)
	   [-c  Number of clusters (default=45)]
	   [-a	Alignments file]
	        [-t Alignments target]
	   [-m	Morphology file]
	   [-d	Dependencies file]
	"
}

while getopts "hf:o:l:a:t:c:m:d:" OPTION; do
	case $OPTION in
		h)
			usage
			exit 1
			;;
		f)
			folder=$OPTARG
			;;
		o)
			out_file=$OPTARG
			;;
		l)
			language=$OPTARG
			;;
		a)
			aligns_file=$OPTARG
			;;
		t)
			aligns_trg=$OPTARG
			;;
		c)
			num_clusters=$OPTARG
			;;
		m)
			morph_file=$OPTARG
			;;
		d)
			deps_file=$OPTARG
			;;
		?)
			usage
			exit
			;;
	esac
done

if [[ -z $folder ]] || [[ -z $language ]] || [[ -z $out_file ]]; then
	usage
	exit 1
fi

raw_file=$folder/$language.raw

if [[ ! -z $aligns_file ]] && [[ ! -z $aligns_trg ]]; then
	# Make sure $folder and $aligns_trg are set
	align_params="-alignFile $aligns_file -alignLangs $aligns_trg -corpusLang $language -langsFileRegexp '$folder/*.raw'"
fi
if [ ! -z $morph_file ]; then
	morph_params="-morph $morph_file"
fi
if [ ! -z $deps_file ]; then
	dep_params="-deps $deps_file"
fi
options="-classes $num_clusters -iters 500 -out $out_file $align_params $morph_params $dep_params"
java -jar bin/bmmm.jar $options $raw_file
