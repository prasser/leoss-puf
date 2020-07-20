# Anonymization pipeline for the LEOSS public use file

This process has been designed for continuous publishing of the LEOSS dataset. It must be called using the following conventions:

java -jar leoss-public-use-file-[version].jar [input].csv [output].csv

It is expected that "input.csv" contains all records that can be released by the registry. The process will output a subset of the records, making
up the complete Public Use File.