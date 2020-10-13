///////////////////////////////////////////////////////////////////////////////////////
///////// > openSMILE configuration file, Geneva feature set <       //////////////////
/////////                                                            //////////////////
///////// (c) 2014 by audEERING                                      //////////////////
/////////     All rights reserved. See file COPYING for details.     //////////////////
///////////////////////////////////////////////////////////////////////////////////////

// Referenced by Tiantian Feng: tiantiaf@usc.edu

;;;;;;;;;;;;;;;;;;;;;;

[componentInstances:cComponentManager]
instance[jelly_frame60].type=cFramer
instance[jelly_win60].type=cWindower
instance[jelly_fft60].type=cTransformFFT
instance[jelly_fftmp60].type=cFFTmagphase


[jelly_frame60:cFramer]
reader.dmLevel=wave
writer.dmLevel=jelly_frame60
\{\cm[bufferModeRbConf{BufferModeRb.conf.inc}:buffer mode for pre-functionals levels]}
frameSize = 0.060
frameStep = 0.010
frameCenterSpecial = left

[jelly_win60:cWindower]
reader.dmLevel=jelly_frame60
writer.dmLevel=jelly_winG60
winFunc=gauss
gain=1.0
sigma=0.4

[jelly_fft60:cTransformFFT]
reader.dmLevel=jelly_winG60
writer.dmLevel=jelly_fftcG60
 ; for compatibility with 2.2.0 and older versions
zeroPadSymmetric = 0

[jelly_fftmp60:cFFTmagphase]
reader.dmLevel=jelly_fftcG60
writer.dmLevel=jelly_fftmagG60
\{\cm[bufferModeRbLagConf{BufferModeRbLag.conf.inc}:path to included config to set the buffer mode for levels which will be joint with Viterbi smoothed -lagged- F0]}


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

[componentInstances:cComponentManager]
instance[jelly_frame25].type=cFramer
instance[jelly_win25].type=cWindower
instance[jelly_fft25].type=cTransformFFT
instance[jelly_fftmp25].type=cFFTmagphase

[jelly_frame25:cFramer]
reader.dmLevel=wave
writer.dmLevel=jelly_frame25
\{\cm[bufferModeRbConf]}
frameSize = 0.020
frameStep = 0.010
frameCenterSpecial = left

[jelly_win25:cWindower]
reader.dmLevel=jelly_frame25
writer.dmLevel=jelly_winH25
winFunc=hamming

[jelly_fft25:cTransformFFT]
reader.dmLevel=jelly_winH25
writer.dmLevel=jelly_fftcH25
 ; for compatibility with 2.2.0 and older versions
zeroPadSymmetric = 0

[jelly_fftmp25:cFFTmagphase]
reader.dmLevel=jelly_fftcH25
writer.dmLevel=jelly_fftmagH25
;;; CHECK!!!!!!!
;writer.levelconf.growDyn = 1
;writer.levelconf.isRb = 0


;;;;;;;;;;;;;;;;;;;; HPS pitch

[componentInstances:cComponentManager]
instance[jelly_scale].type=cSpecScale
instance[jelly_shs].type=cPitchShs

[jelly_scale:cSpecScale]
reader.dmLevel=jelly_fftmagG60
writer.dmLevel=jelly_hpsG60
\{\cm[bufferModeRbConf]}
copyInputName = 1
processArrayFields = 0
scale=octave
sourceScale = lin
interpMethod = spline
minF = 25
maxF = -1
nPointsTarget = 0
specSmooth = 1
specEnhance = 1
auditoryWeighting = 1

[jelly_shs:cPitchShs]
reader.dmLevel=jelly_hpsG60
writer.dmLevel=jelly_pitchShsG60
\{\cm[bufferModeRbLagConf]}
copyInputName = 1
processArrayFields = 0
maxPitch = 1000
minPitch = 55
nCandidates = 6
scores = 1
voicing = 1
F0C1 = 0
voicingC1 = 0
F0raw = 1
voicingClip = 1
voicingCutoff = 0.700000
inputFieldSearch = Mag_octScale
octaveCorrection = 0
nHarmonics = 15
compressionFactor = 0.850000
greedyPeakAlgo = 1

;;;;; Pitch with Viterbi smoother
[componentInstances:cComponentManager]
instance[jelly_energy60].type=cEnergy

[jelly_energy60:cEnergy]
reader.dmLevel=jelly_winG60
writer.dmLevel=jelly_e60
 ; This must be > than buffersize of viterbi smoother
 ; writer.levelconf.nT=100
\{\cm[bufferModeRbLagConf]}
rms=1
log=0


[componentInstances:cComponentManager]
instance[jelly_pitchSmoothViterbi].type=cPitchSmootherViterbi

[jelly_pitchSmoothViterbi:cPitchSmootherViterbi]
reader.dmLevel=jelly_pitchShsG60
reader2.dmLevel=jelly_pitchShsG60
writer.dmLevel=jelly_logPitchRaw
copyInputName = 1
bufferLength=40
F0final = 1
F0finalLog = 1
F0finalEnv = 0
voicingFinalClipped = 0
voicingFinalUnclipped = 1
F0raw = 0
voicingC1 = 0
voicingClip = 0
wTvv =10.0
wTvvd= 5.0
wTvuv=10.0
wThr = 4.0
wTuu = 0.0
wLocal=2.0
wRange=1.0


[componentInstances:cComponentManager]
instance[jelly_volmerge].type = cValbasedSelector

[jelly_volmerge:cValbasedSelector]
reader.dmLevel = jelly_e60;jelly_logPitchRaw
writer.dmLevel = jelly_logPitch
\{\cm[bufferModeRbLagConf]}
idx=0
threshold=0.001
removeIdx=1
zeroVec=1
outputVal=0.0



;;;;;;;;;;;;;;;;;;; Voice Quality (VQ)

[componentInstances:cComponentManager]
instance[jelly_pitchJitter].type=cPitchJitter

[jelly_pitchJitter:cPitchJitter]
reader.dmLevel = wave
writer.dmLevel = jelly_jitterShimmer
\{\cm[bufferModeRbLagConf]}
copyInputName = 1
F0reader.dmLevel = jelly_logPitch
F0field = F0final
searchRangeRel = 0.100000
jitterLocal = 1
jitterDDP = 0
jitterLocalEnv = 0
jitterDDPEnv = 0
shimmerLocal = 0
shimmerLocalDB = 1
shimmerLocalEnv = 0
onlyVoiced = 0
logHNR = 0
 ; This must be larger than the viterbi pitch smoother lag
inputMaxDelaySec = 2.5
minNumPeriods = 2
minCC = 0.5
useBrokenJitterThresh = 1




;;;;;;;;;;;;;;;;;;;;; Energy / loudness


[componentInstances:cComponentManager]
instance[jelly_melspec1].type=cMelspec
instance[jelly_audspec].type=cPlp
instance[jelly_audspecSum].type=cVectorOperation

[jelly_melspec1:cMelspec]
reader.dmLevel=jelly_fftmagH25
writer.dmLevel=jelly_melspec1
; htk compatible sample value scaling
htkcompatible = 0
nBands = 26
; use power spectrum instead of magnitude spectrum
usePower = 1
lofreq = 20
hifreq = 8000
specScale = mel
showFbank = 0

; perform auditory weighting of spectrum
[jelly_audspec:cPlp]
reader.dmLevel=jelly_melspec1
writer.dmLevel=jelly_audspec
firstCC = 0
lpOrder = 5
cepLifter = 22
compression = 0.33
htkcompatible = 0
doIDFT = 0
doLpToCeps = 0
doLP = 0
doInvLog = 0
doAud = 1
doLog = 0
newRASTA=0
RASTA=0

[jelly_audspecSum:cVectorOperation]
reader.dmLevel = jelly_audspec
writer.dmLevel = jelly_loudness
; This must be larger than the F0 viterbi buffer length
; since audspecSum and F0 envelope are joint later!
\{\cm[bufferModeRbLagConf]}
nameAppend = loudness
copyInputName = 0
processArrayFields = 0
operation = ll1
nameBase = loudness


;;;;;;;;;;;;;; Formants ;;;;;;;;;;;;;;;;;;;;;;
[componentInstances:cComponentManager]
instance[jelly_resampLpc].type=cSpecResample
instance[jelly_lpc].type=cLpc
instance[jelly_formantLpc].type=cFormantLpc

[jelly_resampLpc:cSpecResample]
// use fftcG60 ?  H25 has faster resampling
reader.dmLevel=jelly_fftcH25
writer.dmLevel=jelly_outpR
targetFs = 11000

[jelly_lpc:cLpc]
reader.dmLevel=jelly_outpR
writer.dmLevel=jelly_lpc
p=11
method=acf
lpGain=0
saveLPCoeff=1
residual=0
forwardFilter=0
lpSpectrum=0

[jelly_formantLpc:cFormantLpc]
reader.dmLevel=jelly_lpc
writer.dmLevel=jelly_formants
\{\cm[bufferModeRbLagConf]}
saveIntensity=0
saveBandwidths=1
maxF=5500.0
minF=50.0
nFormants=5
useLpSpec=0
medianFilter=0
octaveCorrection=0


;;;;;;;;;;;;; Harmonics ;;;;;;;;;;;;;;;;;;;;
[componentInstances:cComponentManager]
instance[jelly_harmonics].type = cHarmonics

  ;;;; default (template) configuration section for component 'cHarmonics' ;;;;
[jelly_harmonics:cHarmonics]
reader.dmLevel = jelly_logPitch;jelly_formants;jelly_fftmagG60
writer.dmLevel = jelly_harmonics
\{\cm[bufferModeRbLagConf]}
copyInputName = 0
processArrayFields = 0
includeSingleElementFields = 1
preserveFieldNames = 0
formantFrequencyFieldName = formantFreqLpc
formantFrequencyFieldNameIsFull = 1
formantBandwidthFieldName = formantBandwidthLpc
formantBandwidthFieldNameIsFull = 1
f0ElementName = F0final
f0ElementNameIsFull = 1
magSpecFieldName = pcm_fftMag
magSpecFieldNameIsFull = 1
nHarmonics = 100
harmonicDifferences = H1-H2;H1-A3
harmonicDifferencesLog = 1
nHarmonicMagnitudes = 0
firstHarmonicMagnitude = 1
outputLogRelMagnitudes = 1
formantAmplitudes=1
formantAmplitudesLogRel = 1
formantAmplitudesStart = 1
formantAmplitudesEnd = 3
computeAcfHnrLogdB = 1


;;;;;;;;;;;;;;; spectral
[componentInstances:cComponentManager]
instance[jelly_logSpectral].type=cSpectral

[jelly_logSpectral:cSpectral]
reader.dmLevel=jelly_fftmagH25
writer.dmLevel=jelly_logSpectral
\{\cm[bufferModeRbLagConf]}
flux = 0
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
alphaRatio = 1
hammarbergIndex = 1
slope = 0
slopes[0] = 0-500
slopes[1] = 500-1500
  ; NOTE: added this to sync with eGeMAPS set, should have no effect.
normBandEnergies = 1
squareInput = 1
useLogSpectrum = 1
freqRange = 0-5000
oldSlopeScale = 0



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
instance[jelly_lldSetSelectorE].type=cDataSelector
instance[jelly_lldSetSelectorF].type=cDataSelector
instance[jelly_lldSetSelectorLogF0].type=cDataSelector
instance[jelly_formantVoiced].type = cValbasedSelector
instance[jelly_lldSetSelectorNoF0LoudnNz].type = cDataSelector
instance[jelly_logSpectralVoiced].type = cValbasedSelector
instance[jelly_logSpectralUnvoiced].type = cValbasedSelector
instance[jelly_lldSetSelectorSpectralNz].type=cDataSelector
instance[jelly_lldSetSelectorSpectralZ].type=cDataSelector

[jelly_lldSetSelectorE:cDataSelector]
reader.dmLevel = jelly_loudness;jelly_logSpectral
writer.dmLevel = jelly_lldsetE
\{\cm[bufferModeRbConf]}
selected = loudness;pcm_fftMag_alphaRatioDB;pcm_fftMag_hammarbergIndexDB;pcm_fftMag_logSpectralSlopeOfBand0-500;pcm_fftMag_logSpectralSlopeOfBand500-1500
newNames = Loudness;alphaRatio;hammarbergIndex;slope0-500;slope500-1500


[jelly_lldSetSelectorF:cDataSelector]
reader.dmLevel = jelly_logPitch;jelly_jitterShimmer;jelly_harmonics;jelly_formants
writer.dmLevel = jelly_lldsetF
\{\cm[bufferModeRbConf]}
selected = F0finalLog;jitterLocal;shimmerLocalDB;HarmonicsToNoiseRatioACFLogdB;HarmonicDifferenceLogRelH1-H2;HarmonicDifferenceLogRelH1-A3;formantFreqLpc[1];formantBandwidthLpc[1];FormantAmplitudeByMaxHarmonicLogRelF0[1];formantFreqLpc[2];FormantAmplitudeByMaxHarmonicLogRelF0[2];formantFreqLpc[3];FormantAmplitudeByMaxHarmonicLogRelF0[3]
newNames = F0semitoneFrom27.5Hz;jitterLocal;shimmerLocaldB;HNRdBACF;logRelF0-H1-H2;logRelF0-H1-A3;F1frequency;F1bandwidth;F1amplitudeLogRelF0;F2frequency;F2amplitudeLogRelF0;F3frequency;F3amplitudeLogRelF0

[jelly_lldSetSelectorLogF0:cDataSelector]
reader.dmLevel = jelly_logPitch
writer.dmLevel = jelly_lld_single_logF0
\{\cm[bufferModeRbConf]}
selected = F0finalLog
newNames = F0semitoneFrom27.5Hz

// select only formants where F0 > 0 for functionals
// (in LLD csv we output all of them..!)
[jelly_formantVoiced:cValbasedSelector]
reader.dmLevel = jelly_lld_single_logF0;jelly_formants
writer.dmLevel = jelly_formantsNz
\{\cm[bufferModeRbLagConf]}
idx=0
threshold=0.000001
removeIdx=1
zeroVec=1
outputVal=0.0

[jelly_lldSetSelectorNoF0LoudnNz:cDataSelector]
reader.dmLevel = jelly_jitterShimmer;jelly_harmonics;jelly_formantsNz
writer.dmLevel = jelly_lldSetNoF0AndLoudnessNz
\{\cm[bufferModeRbConf]}
selected = jitterLocal;shimmerLocalDB;HarmonicsToNoiseRatioACFLogdB;HarmonicDifferenceLogRelH1-H2;HarmonicDifferenceLogRelH1-A3;formantFreqLpc[1];formantBandwidthLpc[1];FormantAmplitudeByMaxHarmonicLogRelF0[1];formantFreqLpc[2];FormantAmplitudeByMaxHarmonicLogRelF0[2];formantFreqLpc[3];FormantAmplitudeByMaxHarmonicLogRelF0[3]
newNames = jitterLocal;shimmerLocaldB;HNRdBACF;logRelF0-H1-H2;logRelF0-H1-A3;F1frequency;F1bandwidth;F1amplitudeLogRelF0;F2frequency;F2amplitudeLogRelF0;F3frequency;F3amplitudeLogRelF0

// select logspectral for voiced sounds
[jelly_logSpectralVoiced:cValbasedSelector]
reader.dmLevel = jelly_lld_single_logF0;jelly_logSpectral
writer.dmLevel = jelly_logSpectralVoiced
\{\cm[bufferModeRbLagConf]}
idx=0
threshold=0.000001
removeIdx=1
zeroVec=1
outputVal=0.0

// select logspectral for voiced sounds
[jelly_logSpectralUnvoiced:cValbasedSelector]
reader.dmLevel = jelly_lld_single_logF0;jelly_logSpectral
writer.dmLevel = jelly_logSpectralUnvoiced
\{\cm[bufferModeRbLagConf]}
idx=0
invert = 1
threshold = 0.000001
removeIdx=1
zeroVec=1
outputVal=0.0


[jelly_lldSetSelectorSpectralNz:cDataSelector]
reader.dmLevel = jelly_logSpectralVoiced
writer.dmLevel = jelly_lldSetSpectralNz
\{\cm[bufferModeRbConf]}
selected = pcm_fftMag_alphaRatioDB;pcm_fftMag_hammarbergIndexDB;pcm_fftMag_logSpectralSlopeOfBand0-500;pcm_fftMag_logSpectralSlopeOfBand500-1500
newNames = alphaRatioV;hammarbergIndexV;slopeV0-500;slopeV500-1500

[jelly_lldSetSelectorSpectralZ:cDataSelector]
reader.dmLevel = jelly_logSpectralUnvoiced
writer.dmLevel = jelly_lldSetSpectralZ
\{\cm[bufferModeRbConf]}
selected = pcm_fftMag_alphaRatioDB;pcm_fftMag_hammarbergIndexDB;pcm_fftMag_logSpectralSlopeOfBand0-500;pcm_fftMag_logSpectralSlopeOfBand500-1500
newNames = alphaRatioUV;hammarbergIndexUV;slopeUV0-500;slopeUV500-1500


;;;;;;;;;;;;;;;;  smoothing ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
[componentInstances:cComponentManager]
instance[jelly_smoFnz].type=cContourSmoother
instance[jelly_smoE].type=cContourSmoother
instance[jelly_smoNoF0andLoudnNz].type=cContourSmoother
instance[jelly_smoSpectralZ].type=cContourSmoother
instance[jelly_smoSpectralNz].type=cContourSmoother

[jelly_smoFnz:cContourSmoother]
reader.dmLevel = jelly_lldsetF
writer.dmLevel = jelly_lldsetF_smo
nameAppend = sma3nz
copyInputName = 1
noPostEOIprocessing = 0
smaWin = 3
noZeroSma = 1

[jelly_smoE:cContourSmoother]
reader.dmLevel = jelly_lldsetE
writer.dmLevel = jelly_lldsetE_smo
nameAppend = sma3
copyInputName = 1
noPostEOIprocessing = 0
smaWin = 3
noZeroSma = 0

[jelly_smoNoF0andLoudnNz:cContourSmoother]
reader.dmLevel = jelly_lldSetNoF0AndLoudnessNz
writer.dmLevel = jelly_lldSetNoF0AndLoudnessNz_smo
\{\cm[bufferModeConf{BufferMode.conf.inc}:path to included config to set the buffer mode for the levels before the functionals]}
;\{\cm[bufferModeConf{BufferModeLive.conf.inc}:path to included config to set the buffer mode for the levels before the functionals]}
nameAppend = sma3nz
copyInputName = 1
noPostEOIprocessing = 0
smaWin = 3
noZeroSma = 1

[jelly_smoSpectralZ:cContourSmoother]
reader.dmLevel = jelly_lldSetSpectralZ
writer.dmLevel = jelly_lldSetSpectralZ_smo
\{\cm[bufferModeConf]}
nameAppend = sma3nz
copyInputName = 1
noPostEOIprocessing = 0
smaWin = 3
; non-zero SMA is ok here, as it is inverted with 0's for the voiced parts
noZeroSma = 1

[jelly_smoSpectralNz:cContourSmoother]
reader.dmLevel = jelly_lldSetSpectralNz
writer.dmLevel = jelly_lldSetSpectralNz_smo
\{\cm[bufferModeConf]}
nameAppend = sma3nz
copyInputName = 1
noPostEOIprocessing = 0
smaWin = 3
noZeroSma = 1
