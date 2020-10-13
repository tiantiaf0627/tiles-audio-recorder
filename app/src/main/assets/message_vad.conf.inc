// This file is part of openSMILE.
// (c) 2016 by audEERING GmbH. All rights reserved.
// See file COPYING for details on licensing terms.
//
// Written by Florian Eyben. fe@audeering.com

[componentInstances:cComponentManager]
instance[bandVAD].type=cSimpleMessageSender

[bandVAD:cSimpleMessageSender]
reader.dmLevel = isTurn
sendPeriodically = 5
messageName = vad_act
messageType = vad
messageRecp = jniMessageInterface
enableDebugSender = 0


