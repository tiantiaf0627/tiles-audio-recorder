// This file is part of openSMILE.
// (c) 2016 by audEERING GmbH. All rights reserved.
// See file COPYING for details on licensing terms.
//
// Written by Florian Eyben. fe@audeering.com

[componentInstances:cComponentManager]
instance[bandPitch].type=cSimpleMessageSender
instance[bandMFCC].type=cSimpleMessageSender
instance[bandLSP].type=cSimpleMessageSender
instance[bandSpectral].type=cSimpleMessageSender

[bandPitch:cSimpleMessageSender]
;reader.dmLevel = melspec;energyB;pitchSmooth2;pitchJitter
reader.dmLevel = melspec;energyB;pitchF;pitchJitter
sendPeriodically = 2
messageName = pitch_act
messageType = pitch
messageRecp = jniMessageInterface
enableDebugSender = 0

[bandMFCC:cSimpleMessageSender]
reader.dmLevel = mfcc
sendPeriodically = 2
messageName = mfcc_act
messageType = mfcc
messageRecp = jniMessageInterface
enableDebugSender = 0
showCustDataAsText = 1


[bandLSP:cSimpleMessageSender]
reader.dmLevel = lsp
sendPeriodically = 2
messageName = lsp_act
messageType = lsp
messageRecp = jniMessageInterface
enableDebugSender = 0


[bandSpectral:cSimpleMessageSender]
reader.dmLevel = spectral
sendPeriodically = 2
messageName = spectral_act
messageType = spectral
messageRecp = jniMessageInterface
enableDebugSender = 0

