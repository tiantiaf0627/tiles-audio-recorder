// This file is part of openSMILE.
// (c) 2016 by audEERING GmbH. All rights reserved.
// See file COPYING for details on licensing terms.
//
// Originally Written by Florian Eyben. fe@audeering.com
// Revised by Tiantian Feng. tiantiaf@usc.edu

[componentInstances:cComponentManager]
instance[framesB].type=cFramer
instance[energyMeter].type=cEnergy
instance[winB].type=cWindower
instance[fftB].type=cTransformFFT
instance[fftmagB].type=cFFTmagphase
instance[bandspec].type=cMelspec
instance[scale].type=cSpecScale
instance[pitchShs].type=cPitchShs
instance[pitchSmooth2].type=cPitchSmoother
instance[mfcc].type=cMfcc

instance[lpc].type=cLpc
instance[lsp].type=cLsp
instance[res].type=cSpecResample
instance[spectral].type=cSpectral
instance[formants].type=cFormantLpc

[framesB:cFramer]
reader.dmLevel = wave
writer.dmLevel = framesB
\{\cm[bufferModeRbConf{BufferModeRb.conf.inc}:buffer mode for pre-functionals levels]}
frameSize = 0.025
frameStep = 0.010

[energyMeter:cEnergy]
reader.dmLevel=framesB
writer.dmLevel=energyB
log=1
rms=0
ebiasLog = 30
escaleLog = 12

[winB:cWindower]
reader.dmLevel=framesB
writer.dmLevel=windowsB
copyInputName = 1
processArrayFields = 1
 ; hamming window
winFunc = ham

[fftB:cTransformFFT]
reader.dmLevel=windowsB
writer.dmLevel=fftB
copyInputName = 1
processArrayFields = 1
inverse = 0

[fftmagB:cFFTmagphase]
reader.dmLevel=fftB
writer.dmLevel=fftmagB
copyInputName = 1
processArrayFields = 1
inverse = 0
magnitude = 1
phase = 0

[bandspec:cMelspec]
reader.dmLevel=fftmagB
writer.dmLevel=melspec
copyInputName = 1
processArrayFields = 1
htkcompatible = 0
nBands = 3
usePower = 1
lofreq = 0
hifreq = 8000
specScale = bark
inverse = 0

[mfcc:cMfcc]
reader.dmLevel = melspec
writer.dmLevel = mfcc
htkcompatible = 0
firstMfcc=0
lastMfcc=14
cepLifter=22


[scale:cSpecScale]
reader.dmLevel=fftmagB
writer.dmLevel=hps
scale=log
logScaleBase=2

[pitchShs:cPitchShs]
reader.dmLevel=hps
writer.dmLevel=pitchShs
F0raw = 0
voicingClip = 0
voicingC1=0
scores=1
voicing=1
nCandidates=3
octaveCorrection=0

[pitchSmooth2:cPitchSmoother]
reader.dmLevel=pitchShs
writer.dmLevel=pitchF
F0raw = 0
F0final = 1
F0finalEnv = 0
voicingFinalUnclipped = 0
medianFilter0 = 0
postSmoothingMethod = simple
octaveCorrection = 0
writer.levelconf.nT=10
writer.levelconf.isRb=0
writer.levelconf.growDyn=1


[componentInstances:cComponentManager]
instance[pitchJitter].type=cPitchJitter

[pitchJitter:cPitchJitter]
reader.dmLevel = wave
writer.dmLevel = pitchJitter
copyInputName = 1
F0reader.dmLevel = pitchF
F0field = F0final
searchRangeRel = 0.250000
jitterLocal = 1
jitterDDP = 1
jitterLocalEnv = 0
jitterDDPEnv = 0
shimmerLocal = 1
shimmerLocalEnv = 0
onlyVoiced = 0

[res:cSpecResample]
reader.dmLevel=fftB
writer.dmLevel=outpR
targetFs = 11000

[lpc:cLpc]
;reader.dmLevel=framespe
reader.dmLevel=outpR
writer.dmLevel=lpc
p=8
method = acf
saveLPCoeff = 1
lpGain = 0
saveRefCoeff = 0
residual = 0
forwardFilter = 0
lpSpectrum = 0

[formants:cFormantLpc]
reader.dmLevel=lpc
writer.dmLevel=formants
saveIntensity=0
saveBandwidths=1
maxF=5500.0
minF=50.0
nFormants=5
useLpSpec=0
medianFilter=0
octaveCorrection=0

[lsp:cLsp]
reader.dmLevel=lpc
writer.dmLevel=lsp


[spectral:cSpectral]
reader.dmLevel=fftmagB
writer.dmLevel=spectral
copyInputName = 1
processArrayFields = 1
squareInput = 1
bands[0]=0-250
bands[1]=0-650
bands[2]=250-650
bands[3]=1000-4000
;;;bands[4]=3010-9123
rollOff[0] = 0.25
rollOff[1] = 0.50
rollOff[2] = 0.75
rollOff[3] = 0.90
; buggyRollOff = 1
; set the above for compatibility with older releases
flux = 1
centroid = 1
maxPos = 1
minPos = 1
entropy = 0