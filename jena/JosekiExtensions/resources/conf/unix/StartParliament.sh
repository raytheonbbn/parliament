#!/bin/bash

BASE_PATH="`dirname \"$0\"`"                # relative
BASE_PATH="`( cd \"$BASE_PATH\" && pwd )`"  # absolutized and normalized
$BASE_PATH/parliament foreground
