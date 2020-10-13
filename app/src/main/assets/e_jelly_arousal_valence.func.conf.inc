///////////////////////////////////////////////////////////////////////////////////////
///////// > openSMILE configuration file, Geneva feature set <       //////////////////
/////////                                                            //////////////////
///////// (c) 2014 by audEERING                                      //////////////////
/////////     All rights reserved. See file COPYING for details.     //////////////////
///////////////////////////////////////////////////////////////////////////////////////
//  Revised by Tiantian Feng: tiantiaf@usc.edu


[componentInstances:cComponentManager]
instance[e_jelly_leqLin].type= cFunctionals
instance[e_jelly_leq].type = cVectorOperation

;; LEq
[e_jelly_leqLin:cFunctionals]
reader.dmLevel = e_jelly_energyRMS
writer.dmLevel = e_jelly_leqLin
// nameAppend =
preserveFields = 1
\{\cm[bufferModeRbConf]}
copyInputName = 1
\{\cm[frameModeFunctionalsConf]}
noPostEOIprocessing = 0
functionalsEnabled = Means
Means.amean = 1
Means.posamean = 0
Means.absmean = 0
Means.qmean = 0
Means.nzamean = 0
Means.nzabsmean = 0
Means.nzqmean = 0
Means.nzgmean = 0
Means.nnz = 0

[e_jelly_leq:cVectorOperation]
reader.dmLevel = e_jelly_leqLin
writer.dmLevel = e_jelly_leq
nameBase = equivalentSoundLevel
copyInputName = 1
processArrayFields = 0
operation = dBp
appendOperationToName = 1



;;;;;;;;;;;;;;;;;;;;; functionals / summaries ;;;;;;;;;;;;;;;

[componentInstances:cComponentManager]
instance[e_jelly_functionalsMVR].type=cFunctionals
instance[e_jelly_functionalsMeanUV].type=cFunctionals
instance[e_jelly_functionalsMVRVoiced].type = cFunctionals

[e_jelly_functionalsMVR:cFunctionals]
reader.dmLevel = e_jelly_lldSetNoF0AndLoudnessZ_smo
writer.dmLevel = e_jelly_functionalsMeanStddevZ
\{\cm[bufferModeRbConf]}
copyInputName = 1
\{\cm[frameModeFunctionalsConf]}
functionalsEnabled = Moments
Moments.variance = 0
Moments.stddev = 0
Moments.stddevNorm = 2
Moments.skewness = 0
Moments.kurtosis = 0
Moments.amean = 1
nonZeroFuncts = 0
masterTimeNorm = segment

[e_jelly_functionalsMeanUV:cFunctionals]
reader.dmLevel = e_jelly_lldSetSpectralZ_smo
writer.dmLevel = e_jelly_functionalsMeanUnvoiced
\{\cm[bufferModeRbConf]}
copyInputName = 1
\{\cm[frameModeFunctionalsConf]}
functionalsEnabled = Moments
Moments.variance = 0
Moments.stddev = 0
Moments.stddevNorm = 0
Moments.skewness = 0
Moments.kurtosis = 0
Moments.amean = 1
nonZeroFuncts = 1
masterTimeNorm = segment

[e_jelly_functionalsMVRVoiced:cFunctionals]
reader.dmLevel = e_jelly_lldSetNoF0AndLoudnessNz_smo;e_jelly_lldSetSpectralNz_smo
writer.dmLevel = e_jelly_functionalsMeanStddevVoiced
\{\cm[bufferModeRbConf]}
copyInputName = 1
\{\cm[frameModeFunctionalsConf]}
functionalsEnabled = Moments
Moments.variance = 0
Moments.stddev = 0
Moments.stddevNorm = 2
Moments.skewness = 0
Moments.kurtosis = 0
Moments.amean = 1
nonZeroFuncts = 1
masterTimeNorm = segment


