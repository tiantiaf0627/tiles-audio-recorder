///////////////////////////////////////////////////////////////////////////////////////
///////// > openSMILE configuration file, Geneva feature set <       //////////////////
/////////                                                            //////////////////
///////// (c) 2014 by audEERING                                      //////////////////
/////////     All rights reserved. See file COPYING for details.     //////////////////
///////////////////////////////////////////////////////////////////////////////////////
//  Revised by Tiantian Feng: tiantiaf@usc.edu

;; for LEq
[componentInstances:cComponentManager]
instance[e_jelly_energyRMS].type=cEnergy

[e_jelly_energyRMS:cEnergy]
reader.dmLevel = jelly_frame25
writer.dmLevel = e_jelly_energyRMS
\{\cm[bufferModeConf]}
htkcompatible = 0
rms = 0
energy2 = 1
log = 0


;;;;;;;;;;;;;;; spectral
[componentInstances:cComponentManager]
instance[e_jelly_logSpectral_flux].type=cSpectral

[e_jelly_logSpectral_flux:cSpectral]
reader.dmLevel=jelly_fftmagH25
writer.dmLevel=e_jelly_logSpectral_flux
\{\cm[bufferModeRbLagConf]}
flux = 1
centroid = 0
maxPos=0
minPos=0
entropy = 0
flatness = 0
harmonicity = 0
sharpness = 0
variance=0
skewness=0
kurtosis=0
alphaRatio = 0
hammarbergIndex = 0
slope = 0
normBandEnergies = 1
squareInput = 1
useLogSpectrum = 1
freqRange = 0-5000
oldSlopeScale = 0


;;;;;;;;;; cepstral ;;;;;;;;;;;;;;;;;;;
[componentInstances:cComponentManager]
instance[e_jelly_melspecMfcc].type=cMelspec
instance[e_jelly_mfcc].type=cMfcc

[e_jelly_melspecMfcc:cMelspec]
reader.dmLevel=jelly_fftmagH25
writer.dmLevel=e_jelly_melspecMfcc
copyInputName = 1
processArrayFields = 1
; htk compatible sample value scaling
htkcompatible = 1
nBands = 26
; use power spectrum instead of magnitude spectrum
usePower = 1
lofreq = 20
hifreq = 8000
specScale = mel
inverse = 0

[e_jelly_mfcc:cMfcc]
reader.dmLevel=e_jelly_melspecMfcc
writer.dmLevel=e_jelly_mfcc
\{\cm[bufferModeRbLagConf]}
copyInputName = 0
processArrayFields = 1
firstMfcc = 1
lastMfcc  = 4
cepLifter = 22.0
htkcompatible = 1

;;;;;;;;;;;; collecting, filtering, and renaming ;;;;;;;;;;;;;;;;,

/*
logSpectral     Hammarberg, AlphaRatio, spectralSlope  0-500, 500-1500
harmonics   H1-H2, H1-A3, HNRlog
logPitch    F0finalLog
jitterShimmer   jitterLocal, shimmerLocal
loudness
formants   F1-3 freq, F1 bandw (check!)
harmonics  F1-3 level relative
*/

[componentInstances:cComponentManager]
instance[e_jelly_lldSetSelectorE].type=cDataSelector
instance[jelly_lldSetSelectorNoF0LoudnZ].type=cDataSelector
instance[e_jelly_lldSetSelectorNoF0LoudnNz].type = cDataSelector
instance[e_jelly_logSpectralVoiced].type = cValbasedSelector
instance[e_jelly_logSpectralUnvoiced].type = cValbasedSelector
instance[e_jelly_lldSetSelectorSpectralNz].type=cDataSelector
instance[e_jelly_lldSetSelectorSpectralZ].type=cDataSelector

[e_jelly_lldSetSelectorE:cDataSelector]
reader.dmLevel = jelly_loudness;jelly_logSpectral;e_jelly_logSpectral_flux;e_jelly_mfcc
writer.dmLevel = e_jelly_lldsetE
\{\cm[bufferModeRbConf]}
selected = loudness;pcm_fftMag_alphaRatioDB;pcm_fftMag_hammarbergIndexDB;pcm_fftMag_logSpectralSlopeOfBand0-500;pcm_fftMag_logSpectralSlopeOfBand500-1500;pcm_fftMag_spectralFlux;mfcc[1];mfcc[2];mfcc[3];mfcc[4]
newNames = Loudness;alphaRatio;hammarbergIndex;slope0-500;slope500-1500;spectralFlux;mfcc1;mfcc2;mfcc3;mfcc4

[e_jelly_lldSetSelectorNoF0LoudnNz:cDataSelector]
reader.dmLevel = jelly_jitterShimmer;jelly_harmonics;jelly_formantsNz
writer.dmLevel = jelly_lldSetNoF0AndLoudnessNz
\{\cm[bufferModeRbConf]}
selected = jitterLocal;shimmerLocalDB;HarmonicsToNoiseRatioACFLogdB;HarmonicDifferenceLogRelH1-H2;HarmonicDifferenceLogRelH1-A3;formantFreqLpc[1];formantBandwidthLpc[1];FormantAmplitudeByMaxHarmonicLogRelF0[1];formantFreqLpc[2];FormantAmplitudeByMaxHarmonicLogRelF0[2];formantFreqLpc[3];FormantAmplitudeByMaxHarmonicLogRelF0[3]
newNames = jitterLocal;shimmerLocaldB;HNRdBACF;logRelF0-H1-H2;logRelF0-H1-A3;F1frequency;F1bandwidth;F1amplitudeLogRelF0;F2frequency;F2amplitudeLogRelF0;F3frequency;F3amplitudeLogRelF0

[jelly_lldSetSelectorNoF0LoudnZ:cDataSelector]
 ; gemapsv01a_logSpectral
reader.dmLevel = e_jelly_logSpectral_flux;e_jelly_mfcc
writer.dmLevel = e_jelly_lldSetNoF0AndLoudnessZ
\{\cm[bufferModeRbConf]}
selected = pcm_fftMag_spectralFlux;mfcc[1];mfcc[2];mfcc[3];mfcc[4]
newNames = spectralFlux;mfcc1;mfcc2;mfcc3;mfcc4

[e_jelly_lldSetSelectorNoF0LoudnNz:cDataSelector]
reader.dmLevel = jelly_jitterShimmer;jelly_harmonics;jelly_formantsNz
writer.dmLevel = e_jelly_lldSetNoF0AndLoudnessNz
\{\cm[bufferModeRbConf]}
selected = jitterLocal;shimmerLocalDB;HarmonicsToNoiseRatioACFLogdB;HarmonicDifferenceLogRelH1-H2;HarmonicDifferenceLogRelH1-A3;formantFreqLpc[1];formantBandwidthLpc[1];FormantAmplitudeByMaxHarmonicLogRelF0[1];formantFreqLpc[2];formantBandwidthLpc[2];FormantAmplitudeByMaxHarmonicLogRelF0[2];formantFreqLpc[3];formantBandwidthLpc[3];FormantAmplitudeByMaxHarmonicLogRelF0[3]
newNames = jitterLocal;shimmerLocaldB;HNRdBACF;logRelF0-H1-H2;logRelF0-H1-A3;F1frequency;F1bandwidth;F1amplitudeLogRelF0;F2frequency;F2bandwidth;F2amplitudeLogRelF0;F3frequency;F3bandwidth;F3amplitudeLogRelF0

// select logspectral and mfcc for voiced sounds
[e_jelly_logSpectralVoiced:cValbasedSelector]
reader.dmLevel = jelly_lld_single_logF0;jelly_logSpectral;e_jelly_logSpectral_flux;e_jelly_mfcc
writer.dmLevel = e_jelly_logSpectralVoiced
\{\cm[bufferModeRbLagConf]}
idx=0
threshold=0.000001
removeIdx=1
zeroVec=1
outputVal=0.0

// select logspectral and mfcc for voiced sounds
[e_jelly_logSpectralUnvoiced:cValbasedSelector]
reader.dmLevel = jelly_lld_single_logF0;jelly_logSpectral;e_jelly_logSpectral_flux
writer.dmLevel = e_jelly_logSpectralUnvoiced
\{\cm[bufferModeRbLagConf]}
idx=0
invert = 1
threshold = 0.000001
removeIdx=1
zeroVec=1
outputVal=0.0

[e_jelly_lldSetSelectorSpectralNz:cDataSelector]
reader.dmLevel = e_jelly_logSpectralVoiced
writer.dmLevel = e_jelly_lldSetSpectralNz
\{\cm[bufferModeRbConf]}
selected = pcm_fftMag_alphaRatioDB;pcm_fftMag_hammarbergIndexDB;pcm_fftMag_logSpectralSlopeOfBand0-500;pcm_fftMag_logSpectralSlopeOfBand500-1500;pcm_fftMag_spectralFlux;mfcc[1];mfcc[2];mfcc[3];mfcc[4]
newNames = alphaRatioV;hammarbergIndexV;slopeV0-500;slopeV500-1500;spectralFluxV;mfcc1V;mfcc2V;mfcc3V;mfcc4V

[e_jelly_lldSetSelectorSpectralZ:cDataSelector]
reader.dmLevel = e_jelly_logSpectralUnvoiced
writer.dmLevel = e_jelly_lldSetSpectralZ
\{\cm[bufferModeRbConf]}
selected = pcm_fftMag_alphaRatioDB;pcm_fftMag_hammarbergIndexDB;pcm_fftMag_logSpectralSlopeOfBand0-500;pcm_fftMag_logSpectralSlopeOfBand500-1500;pcm_fftMag_spectralFlux
newNames = alphaRatioUV;hammarbergIndexUV;slopeUV0-500;slopeUV500-1500;spectralFluxUV


;;;;;;;;;;;;;;;;  smoothing ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
[componentInstances:cComponentManager]
instance[e_jelly_smoE].type=cContourSmoother
instance[e_jelly_smoNoFLZ].type=cContourSmoother
instance[e_jelly_smoNoF0andLoudnNz].type=cContourSmoother
instance[e_jelly_smoSpectralZ].type=cContourSmoother
instance[e_jelly_smoSpectralNz].type=cContourSmoother

[e_jelly_smoE:cContourSmoother]
reader.dmLevel = e_jelly_lldsetE
writer.dmLevel = e_jelly_lldsetE_smo
nameAppend = sma3
copyInputName = 1
noPostEOIprocessing = 0
smaWin = 3
noZeroSma = 0

[e_jelly_smoNoFLZ:cContourSmoother]
reader.dmLevel = e_jelly_lldSetNoF0AndLoudnessZ
writer.dmLevel = e_jelly_lldSetNoF0AndLoudnessZ_smo
\{\cm[bufferModeConf]}
nameAppend = sma3
copyInputName = 1
noPostEOIprocessing = 0
smaWin = 3
noZeroSma = 0


[e_jelly_smoNoF0andLoudnNz:cContourSmoother]
reader.dmLevel = e_jelly_lldSetNoF0AndLoudnessNz
writer.dmLevel = e_jelly_lldSetNoF0AndLoudnessNz_smo
\{\cm[bufferModeConf]}
nameAppend = sma3nz
copyInputName = 1
noPostEOIprocessing = 0
smaWin = 3
noZeroSma = 1

[e_jelly_smoSpectralZ:cContourSmoother]
reader.dmLevel = e_jelly_lldSetSpectralZ
writer.dmLevel = e_jelly_lldSetSpectralZ_smo
\{\cm[bufferModeConf]}
nameAppend = sma3nz
copyInputName = 1
noPostEOIprocessing = 0
smaWin = 3
; non-zero SMA is ok here, as it is inverted with 0's for the voiced parts
noZeroSma = 1

[e_jelly_smoSpectralNz:cContourSmoother]
reader.dmLevel = e_jelly_lldSetSpectralNz
writer.dmLevel = e_jelly_lldSetSpectralNz_smo
\{\cm[bufferModeConf]}
nameAppend = sma3nz
copyInputName = 1
noPostEOIprocessing = 0
smaWin = 3
noZeroSma = 1

