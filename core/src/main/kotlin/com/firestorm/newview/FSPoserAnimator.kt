package com.firestorm.newview

import java.util.UUID

enum class EBoneTypes {
    WHOLEAVATAR,
    BODY,
    FACE,
    HANDS,
    MISC,
    COL_VOLUMES,
}

enum class EBoneDeflectionStyles {
    NONE,
    MIRROR,
    SYMPATHETIC,
    DELTAMODE,
    MIRROR_DELTA,
    SYMPATHETIC_DELTA,
}

enum class ERotationStyle {
    ABSOLUTE_ROT,
    DELTAIC_ROT,
}

enum class EBoneAxisTranslation {
    SWAP_NOTHING,
    SWAP_YAW_AND_ROLL,
    SWAP_YAW_AND_PITCH,
    SWAP_ROLL_AND_PITCH,
    SWAP_X2Y_Y2Z_Z2X,
    SWAP_X2Z_Y2X_Z2Y,
}

enum class EBoneAxisNegation(val bits: Int) {
    NEGATE_NOTHING(0),
    NEGATE_YAW(1),
    NEGATE_PITCH(2),
    NEGATE_ROLL(4),
    NEGATE_ALL(8),
}

open class FSPoserAnimator {

    class FSPoserJoint(
        private val jointNameVal: String,
        private val mirrorJointNameVal: String,
        private val boneListVal: EBoneTypes,
        private val bvhChildrenVal: List<String> = emptyList(),
        private val bvhOffsetVal: String = "",
        private val bvhEndSiteOffsetVal: String = "",
        private val dontFlipVal: Boolean = false,
    ) {
        fun jointName(): String = jointNameVal
        fun mirrorJointName(): String = mirrorJointNameVal
        fun boneType(): EBoneTypes = boneListVal
        fun dontFlipOnMirror(): Boolean = dontFlipVal
        fun bvhChildren(): List<String> = bvhChildrenVal
        fun bvhOffset(): String = bvhOffsetVal
        fun bvhEndSite(): String = bvhEndSiteOffsetVal
    }

    val poserJoints: List<FSPoserJoint> = listOf(
        // head, torso, legs
        FSPoserJoint("mHead", "", EBoneTypes.BODY, listOf("mEyeLeft","mEyeRight","mFaceRoot","mSkull","HEAD"), "0.000 0.076 0.000"),
        FSPoserJoint("mNeck", "", EBoneTypes.BODY, listOf("mHead","NECK"), "0.000 0.251 -0.010"),
        FSPoserJoint("mPelvis", "", EBoneTypes.WHOLEAVATAR, listOf("mSpine1","mHipLeft","mHipRight","mTail1","mGroin","mHindLimbsRoot","PELVIS","BUTT"), "0.000000 0.000000 0.000000"),
        FSPoserJoint("mChest", "", EBoneTypes.BODY, listOf("mNeck","mCollarLeft","mCollarRight","mWingsRoot","CHEST","LEFT_PEC","RIGHT_PEC","UPPER_BACK"), "0.000 0.205 -0.015"),
        FSPoserJoint("mTorso", "", EBoneTypes.BODY, listOf("mSpine3","BELLY","LEFT_HANDLE","RIGHT_HANDLE","LOWER_BACK"), "0.000 0.084 0.000"),
        FSPoserJoint("mCollarLeft","mCollarRight", EBoneTypes.BODY, listOf("mShoulderLeft","L_CLAVICLE"), "0.085 0.165 -0.021"),
        FSPoserJoint("mShoulderLeft","mShoulderRight", EBoneTypes.BODY, listOf("mElbowLeft","L_UPPER_ARM"), "0.079 0.000 0.000"),
        FSPoserJoint("mElbowLeft","mElbowRight", EBoneTypes.BODY, listOf("mWristLeft","L_LOWER_ARM"), "0.248 0.000 0.000"),
        FSPoserJoint("mWristLeft","mWristRight", EBoneTypes.BODY, listOf("mHandThumb1Left","mHandIndex1Left","mHandMiddle1Left","mHandRing1Left","mHandPinky1Left","L_HAND"), "0.205 0.000 0.000"),
        FSPoserJoint("mCollarRight","mCollarLeft", EBoneTypes.BODY, listOf("mShoulderRight","R_CLAVICLE"), "-0.085 0.165 -0.021", "", true),
        FSPoserJoint("mShoulderRight","mShoulderLeft", EBoneTypes.BODY, listOf("mElbowRight","R_UPPER_ARM"), "-0.079 0.000 0.000", "", true),
        FSPoserJoint("mElbowRight","mElbowLeft", EBoneTypes.BODY, listOf("mWristRight","R_LOWER_ARM"), "-0.248 0.000 0.000", "", true),
        FSPoserJoint("mWristRight","mWristLeft", EBoneTypes.BODY, listOf("mHandThumb1Right","mHandIndex1Right","mHandMiddle1Right","mHandRing1Right","mHandPinky1Right","R_HAND"), "-0.205 0.000 0.000", "", true),
        FSPoserJoint("mHipLeft","mHipRight", EBoneTypes.BODY, listOf("mKneeLeft","L_UPPER_LEG"), "0.127 -0.041 0.034"),
        FSPoserJoint("mKneeLeft","mKneeRight", EBoneTypes.BODY, listOf("mAnkleLeft","L_LOWER_LEG"), "-0.046 -0.491 -0.001"),
        FSPoserJoint("mAnkleLeft","mAnkleRight", EBoneTypes.BODY, listOf("mFootLeft","L_FOOT"), "0.001 -0.468 -0.029"),
        FSPoserJoint("mFootLeft","mFootRight", EBoneTypes.BODY, listOf("mToeLeft"), "0.000 -0.061 0.112"),
        FSPoserJoint("mToeLeft","mToeRight", EBoneTypes.BODY, emptyList(), "0.000 0.000 0.109", "0.000 0.020 0.000"),
        FSPoserJoint("mHipRight","mHipLeft", EBoneTypes.BODY, listOf("mKneeRight","R_UPPER_LEG"), "-0.129 -0.041 0.034", "", true),
        FSPoserJoint("mKneeRight","mKneeLeft", EBoneTypes.BODY, listOf("mAnkleRight","R_LOWER_LEG"), "0.049 -0.491 -0.001", "", true),
        FSPoserJoint("mAnkleRight","mAnkleLeft", EBoneTypes.BODY, listOf("mFootRight","R_FOOT"), "0.000 -0.468 -0.029", "", true),
        FSPoserJoint("mFootRight","mFootLeft", EBoneTypes.BODY, listOf("mToeRight"), "0.000 -0.061 0.112", "", true),
        FSPoserJoint("mToeRight","mToeLeft", EBoneTypes.BODY, emptyList(), "0.000 0.000 0.109", "0.000 0.020 0.000", true),
        // face
        FSPoserJoint("mFaceRoot","",EBoneTypes.FACE,listOf("mFaceForeheadLeft","mFaceForeheadCenter","mFaceForeheadRight","mFaceEyebrowOuterLeft","mFaceEyebrowCenterLeft","mFaceEyebrowInnerLeft","mFaceEyebrowOuterRight","mFaceEyebrowCenterRight","mFaceEyebrowInnerRight","mFaceEyeLidUpperLeft","mFaceEyeLidLowerLeft","mFaceEyecornerInnerLeft","mFaceEyeLidUpperRight","mFaceEyeLidLowerRight","mFaceEyecornerInnerRight","mFaceEar1Left","mFaceEar1Right","mFaceNoseBase","mFaceNoseBridge","mFaceNoseLeft","mFaceNoseCenter","mFaceNoseRight","mFaceCheekUpperLeft","mFaceCheekLowerLeft","mFaceCheekUpperRight","mFaceCheekLowerRight","mFaceJaw","mFaceTeethUpper"),"0.000 0.045 0.025"),
        FSPoserJoint("mFaceForeheadLeft","mFaceForeheadRight",EBoneTypes.FACE,emptyList(),"0.035 0.083 0.061","0.004 0.018 0.024"),
        FSPoserJoint("mFaceForeheadCenter","",EBoneTypes.FACE,emptyList(),"0.000 0.065 0.069","0.000 0.000 0.036"),
        FSPoserJoint("mFaceForeheadRight","mFaceForeheadLeft",EBoneTypes.FACE,emptyList(),"-0.035 0.083 0.061","-0.004 0.018 0.024",true),
        FSPoserJoint("mFaceEyebrowOuterLeft","mFaceEyebrowOuterRight",EBoneTypes.FACE,emptyList(),"0.051 0.048 0.064","0.013 0.000 0.023"),
        FSPoserJoint("mFaceEyebrowCenterLeft","mFaceEyebrowCenterRight",EBoneTypes.FACE,emptyList(),"0.043 0.056 0.070","0.000 0.000 0.027"),
        FSPoserJoint("mFaceEyebrowInnerLeft","mFaceEyebrowInnerRight",EBoneTypes.FACE,emptyList(),"0.022 0.051 0.075","0.000 0.000 0.026"),
        FSPoserJoint("mFaceEyebrowOuterRight","mFaceEyebrowOuterLeft",EBoneTypes.FACE,emptyList(),"-0.051 0.048 0.064","-0.013 0.000 0.023",true),
        FSPoserJoint("mFaceEyebrowCenterRight","mFaceEyebrowCenterLeft",EBoneTypes.FACE,emptyList(),"-0.043 0.056 0.070","0.000 0.000 0.027",true),
        FSPoserJoint("mFaceEyebrowInnerRight","mFaceEyebrowInnerLeft",EBoneTypes.FACE,emptyList(),"-0.022 0.051 0.075","0.000 0.000 0.026",true),
        FSPoserJoint("mEyeLeft","mEyeRight",EBoneTypes.FACE,emptyList(),"-0.036 0.079 0.098","0.000 0.000 0.025"),
        FSPoserJoint("mEyeRight","mEyeLeft",EBoneTypes.FACE,emptyList(),"0.036 0.079 0.098","0.000 0.000 0.025",true),
        FSPoserJoint("mFaceEyeLidUpperLeft","mFaceEyeLidUpperRight",EBoneTypes.FACE,emptyList(),"0.036 0.034 0.073","0.000 0.005 0.027"),
        FSPoserJoint("mFaceEyecornerInnerLeft","mFaceEyecornerInnerRight",EBoneTypes.FACE,emptyList(),"0.032 0.075 0.017","0.000 0.016 0.000"),
        FSPoserJoint("mFaceEyeLidLowerLeft","mFaceEyeLidLowerRight",EBoneTypes.FACE,emptyList(),"0.036 0.034 0.073","0.000 -0.007 0.024"),
        FSPoserJoint("mFaceEyeLidUpperRight","mFaceEyeLidUpperLeft",EBoneTypes.FACE,emptyList(),"-0.036 0.034 0.073","0.000 0.005 0.027",true),
        FSPoserJoint("mFaceEyecornerInnerRight","mFaceEyecornerInnerLeft",EBoneTypes.FACE,emptyList(),"0.032 0.075 -0.017","0.000 0.016 0.000",true),
        FSPoserJoint("mFaceEyeLidLowerRight","mFaceEyeLidLowerLeft",EBoneTypes.FACE,emptyList(),"-0.036 0.034 0.073","0.000 -0.007 0.024",true),
        FSPoserJoint("mFaceEar1Left","mFaceEar1Right",EBoneTypes.FACE,listOf("mFaceEar2Left"),"0.080 0.002 0.000",""),
        FSPoserJoint("mFaceEar2Left","mFaceEar2Right",EBoneTypes.FACE,emptyList(),"0.018 0.025 -0.019","0.000 0.033 0.000"),
        FSPoserJoint("mFaceEar1Right","mFaceEar1Left",EBoneTypes.FACE,listOf("mFaceEar2Right"),"-0.080 0.002 0.000","",true),
        FSPoserJoint("mFaceEar2Right","mFaceEar2Left",EBoneTypes.FACE,emptyList(),"-0.018 0.025 -0.019","0.000 0.033 0.000",true),
        FSPoserJoint("mFaceNoseBase","",EBoneTypes.FACE,emptyList(),"-0.016 0.094 0.000","0.000 0.014 0.000"),
        FSPoserJoint("mFaceNoseBridge","",EBoneTypes.FACE,emptyList(),"0.020 0.091 0.000","0.008 0.015 0.000"),
        FSPoserJoint("mFaceNoseLeft","mFaceNoseRight",EBoneTypes.FACE,emptyList(),"0.015 -0.004 0.086","0.004 0.000 0.015"),
        FSPoserJoint("mFaceNoseCenter","",EBoneTypes.FACE,emptyList(),"0.000 0.000 0.102","0.000 0.000 0.025"),
        FSPoserJoint("mFaceNoseRight","mFaceNoseLeft",EBoneTypes.FACE,emptyList(),"-0.015 -0.004 0.086","-0.004 0.000 0.015",true),
        FSPoserJoint("mFaceCheekUpperLeft","mFaceCheekUpperRight",EBoneTypes.FACE,emptyList(),"0.034 -0.005 0.070","0.015 0.000 0.022"),
        FSPoserJoint("mFaceCheekLowerLeft","mFaceCheekLowerRight",EBoneTypes.FACE,emptyList(),"0.034 -0.031 0.050","0.030 0.000 0.013"),
        FSPoserJoint("mFaceCheekUpperRight","mFaceCheekUpperLeft",EBoneTypes.FACE,emptyList(),"-0.034 -0.005 0.070","-0.015 0.000 0.022",true),
        FSPoserJoint("mFaceCheekLowerRight","mFaceCheekLowerLeft",EBoneTypes.FACE,emptyList(),"-0.034 -0.031 0.050","-0.030 0.000 0.013",true),
        FSPoserJoint("mFaceLipUpperLeft","mFaceLipUpperRight",EBoneTypes.FACE,emptyList(),"0.000 -0.003 0.045","0.015 0.000 0.041"),
        FSPoserJoint("mFaceLipUpperCenter","",EBoneTypes.FACE,emptyList(),"0.000 -0.003 0.045","0.000 0.002 0.043"),
        FSPoserJoint("mFaceLipUpperRight","mFaceLipUpperLeft",EBoneTypes.FACE,emptyList(),"0.000 -0.003 0.045","-0.015 0.000 0.041",true),
        FSPoserJoint("mFaceLipCornerLeft","mFaceLipCornerRight",EBoneTypes.FACE,emptyList(),"-0.019 -0.010 0.028","0.051 0.000 0.045"),
        FSPoserJoint("mFaceLipCornerRight","mFaceLipCornerLeft",EBoneTypes.FACE,emptyList(),"0.019 -0.010 0.028","-0.051 0.000 0.045",true),
        FSPoserJoint("mFaceTeethUpper","",EBoneTypes.FACE,listOf("mFaceLipUpperLeft","mFaceLipUpperCenter","mFaceLipUpperRight","mFaceLipCornerLeft","mFaceLipCornerRight"),"0.000 -0.030 0.020"),
        FSPoserJoint("mFaceTeethLower","",EBoneTypes.FACE,listOf("mFaceLipLowerLeft","mFaceLipLowerCenter","mFaceLipLowerRight","mFaceTongueBase"),"0.000 -0.039 0.021"),
        FSPoserJoint("mFaceTongueBase","",EBoneTypes.FACE,listOf("mFaceTongueTip"),"0.000 0.005 0.039"),
        FSPoserJoint("mFaceTongueTip","",EBoneTypes.FACE,emptyList(),"0.000 0.007 0.022","0.000 0.000 0.010",true),
        FSPoserJoint("mFaceLipLowerLeft","mFaceLipLowerRight",EBoneTypes.FACE,emptyList(),"0.000 0.000 0.045","0.017 0.005 0.034"),
        FSPoserJoint("mFaceLipLowerCenter","",EBoneTypes.FACE,emptyList(),"0.000 0.000 0.045","0.000 0.002 0.040"),
        FSPoserJoint("mFaceLipLowerRight","mFaceLipLowerLeft",EBoneTypes.FACE,emptyList(),"0.000 0.000 0.045","-0.017 0.005 0.034",true),
        FSPoserJoint("mFaceJaw","",EBoneTypes.FACE,listOf("mFaceChin","mFaceTeethLower"),"0.000 -0.015 -0.001",""),
        FSPoserJoint("mFaceChin","",EBoneTypes.FACE,emptyList(),"0.000 -0.015 -0.001","0.000 -0.018 0.021"),
        // left hand
        FSPoserJoint("mHandThumb1Left","mHandThumb1Right",EBoneTypes.HANDS,listOf("mHandThumb2Left"),"0.026 0.004 0.031"),
        FSPoserJoint("mHandThumb2Left","mHandThumb2Right",EBoneTypes.HANDS,listOf("mHandThumb3Left"),"0.032 -0.001 0.028"),
        FSPoserJoint("mHandThumb3Left","mHandThumb3Right",EBoneTypes.HANDS,emptyList(),"0.031 -0.001 0.023","0.025 0.000 0.015"),
        FSPoserJoint("mHandIndex1Left","mHandIndex1Right",EBoneTypes.HANDS,listOf("mHandIndex2Left"),"0.097 0.015 0.038"),
        FSPoserJoint("mHandIndex2Left","mHandIndex2Right",EBoneTypes.HANDS,listOf("mHandIndex3Left"),"0.036 -0.006 0.017"),
        FSPoserJoint("mHandIndex3Left","mHandIndex3Right",EBoneTypes.HANDS,emptyList(),"0.032 -0.006 0.014","0.025 -0.004 0.011"),
        FSPoserJoint("mHandMiddle1Left","mHandMiddle1Right",EBoneTypes.HANDS,listOf("mHandMiddle2Left"),"0.101 0.015 0.013"),
        FSPoserJoint("mHandMiddle2Left","mHandMiddle2Right",EBoneTypes.HANDS,listOf("mHandMiddle3Left"),"0.040 -0.006 -0.001"),
        FSPoserJoint("mHandMiddle3Left","mHandMiddle3Right",EBoneTypes.HANDS,emptyList(),"0.049 -0.008 -0.001","0.033 -0.006 -0.002"),
        FSPoserJoint("mHandRing1Left","mHandRing1Right",EBoneTypes.HANDS,listOf("mHandRing2Left"),"0.099 0.009 -0.010"),
        FSPoserJoint("mHandRing2Left","mHandRing2Right",EBoneTypes.HANDS,listOf("mHandRing3Left"),"0.038 -0.008 -0.013"),
        FSPoserJoint("mHandRing3Left","mHandRing3Right",EBoneTypes.HANDS,emptyList(),"0.040 -0.009 -0.013","0.028 -0.006 -0.010"),
        FSPoserJoint("mHandPinky1Left","mHandPinky1Right",EBoneTypes.HANDS,listOf("mHandPinky2Left"),"0.095 0.003 -0.031"),
        FSPoserJoint("mHandPinky2Left","mHandPinky2Right",EBoneTypes.HANDS,listOf("mHandPinky3Left"),"0.025 -0.006 -0.024"),
        FSPoserJoint("mHandPinky3Left","mHandPinky3Right",EBoneTypes.HANDS,emptyList(),"0.018 -0.004 -0.015","0.016 -0.004 -0.013"),
        // right hand
        FSPoserJoint("mHandThumb1Right","mHandThumb1Left",EBoneTypes.HANDS,listOf("mHandThumb2Right"),"-0.026 0.004 0.031","",true),
        FSPoserJoint("mHandThumb2Right","mHandThumb2Left",EBoneTypes.HANDS,listOf("mHandThumb3Right"),"-0.032 -0.001 0.028","",true),
        FSPoserJoint("mHandThumb3Right","mHandThumb3Left",EBoneTypes.HANDS,emptyList(),"-0.031 -0.001 0.023","-0.025 0.000 0.015",true),
        FSPoserJoint("mHandIndex1Right","mHandIndex1Left",EBoneTypes.HANDS,listOf("mHandIndex2Right"),"-0.097 0.015 0.038","",true),
        FSPoserJoint("mHandIndex2Right","mHandIndex2Left",EBoneTypes.HANDS,listOf("mHandIndex3Right"),"-0.036 -0.006 0.017","",true),
        FSPoserJoint("mHandIndex3Right","mHandIndex3Left",EBoneTypes.HANDS,emptyList(),"-0.032 -0.006 0.014","-0.025 -0.004 0.011",true),
        FSPoserJoint("mHandMiddle1Right","mHandMiddle1Left",EBoneTypes.HANDS,listOf("mHandMiddle2Right"),"-0.101 0.015 0.013","",true),
        FSPoserJoint("mHandMiddle2Right","mHandMiddle2Left",EBoneTypes.HANDS,listOf("mHandMiddle3Right"),"-0.040 -0.006 -0.001","",true),
        FSPoserJoint("mHandMiddle3Right","mHandMiddle3Left",EBoneTypes.HANDS,emptyList(),"-0.049 -0.008 -0.001","-0.033 -0.006 -0.002",true),
        FSPoserJoint("mHandRing1Right","mHandRing1Left",EBoneTypes.HANDS,listOf("mHandRing2Right"),"-0.099 0.009 -0.010","",true),
        FSPoserJoint("mHandRing2Right","mHandRing2Left",EBoneTypes.HANDS,listOf("mHandRing3Right"),"-0.038 -0.008 -0.013","",true),
        FSPoserJoint("mHandRing3Right","mHandRing3Left",EBoneTypes.HANDS,emptyList(),"-0.040 -0.009 -0.013","-0.028 -0.006 -0.010",true),
        FSPoserJoint("mHandPinky1Right","mHandPinky1Left",EBoneTypes.HANDS,listOf("mHandPinky2Right"),"-0.095 0.003 -0.031","",true),
        FSPoserJoint("mHandPinky2Right","mHandPinky2Left",EBoneTypes.HANDS,listOf("mHandPinky3Right"),"-0.025 -0.006 -0.024","",true),
        FSPoserJoint("mHandPinky3Right","mHandPinky3Left",EBoneTypes.HANDS,emptyList(),"-0.018 -0.004 -0.015","-0.016 -0.004 -0.013",true),
        // tail and hind limbs
        FSPoserJoint("mTail1","",EBoneTypes.MISC,listOf("mTail2"),"0.000 0.047 -0.116"),
        FSPoserJoint("mTail2","",EBoneTypes.MISC,listOf("mTail3"),"0.000 0.000 -0.197"),
        FSPoserJoint("mTail3","",EBoneTypes.MISC,listOf("mTail4"),"0.000 0.000 -0.168"),
        FSPoserJoint("mTail4","",EBoneTypes.MISC,listOf("mTail5"),"0.000 0.000 -0.142"),
        FSPoserJoint("mTail5","",EBoneTypes.MISC,listOf("mTail6"),"0.000 0.000 -0.112"),
        FSPoserJoint("mTail6","",EBoneTypes.MISC,emptyList(),"0.000 0.000 -0.094","0.000 0.000 -0.089"),
        FSPoserJoint("mGroin","",EBoneTypes.MISC,emptyList(),"0.000 -0.097 0.064","0.000 -0.066 0.004"),
        FSPoserJoint("mHindLimbsRoot","",EBoneTypes.MISC,listOf("mHindLimb1Left","mHindLimb1Right"),"0.000 0.084 -0.200"),
        FSPoserJoint("mHindLimb1Left","mHindLimb1Right",EBoneTypes.MISC,listOf("mHindLimb2Left"),"0.129 -0.125 -0.204"),
        FSPoserJoint("mHindLimb2Left","mHindLimb2Right",EBoneTypes.MISC,listOf("mHindLimb3Left"),"-0.046 -0.491 0.002"),
        FSPoserJoint("mHindLimb3Left","mHindLimb3Right",EBoneTypes.MISC,listOf("mHindLimb4Left"),"-0.003 -0.468 -0.030"),
        FSPoserJoint("mHindLimb4Left","mHindLimb4Right",EBoneTypes.MISC,emptyList(),"0.000 -0.061 0.112","0.008 0.000 0.105"),
        FSPoserJoint("mHindLimb1Right","mHindLimb1Left",EBoneTypes.MISC,listOf("mHindLimb2Right"),"-0.129 -0.125 -0.204","",true),
        FSPoserJoint("mHindLimb2Right","mHindLimb2Left",EBoneTypes.MISC,listOf("mHindLimb3Right"),"0.046 -0.491 0.002","",true),
        FSPoserJoint("mHindLimb3Right","mHindLimb3Left",EBoneTypes.MISC,listOf("mHindLimb4Right"),"0.003 -0.468 -0.030","",true),
        FSPoserJoint("mHindLimb4Right","mHindLimb4Left",EBoneTypes.MISC,emptyList(),"0.000 -0.061 0.112","-0.008 0.000 0.105",true),
        // wings
        FSPoserJoint("mWingsRoot","",EBoneTypes.MISC,listOf("mWing1Left","mWing1Right"),"0.000 0.000 -0.014"),
        FSPoserJoint("mWing1Left","mWing1Right",EBoneTypes.MISC,listOf("mWing2Left"),"0.105 0.181 -0.099"),
        FSPoserJoint("mWing2Left","mWing2Right",EBoneTypes.MISC,listOf("mWing3Left"),"0.169 0.067 -0.168"),
        FSPoserJoint("mWing3Left","mWing3Right",EBoneTypes.MISC,listOf("mWing4Left","mWing4FanLeft"),"0.183 0.000 -0.181"),
        FSPoserJoint("mWing4Left","mWing4Right",EBoneTypes.MISC,emptyList(),"0.173 0.000 -0.171","0.132 0.000 -0.146"),
        FSPoserJoint("mWing4FanLeft","mWing4FanRight",EBoneTypes.MISC,emptyList(),"0.173 0.000 -0.171","0.062 -0.159 -0.068"),
        FSPoserJoint("mWing1Right","mWing1Left",EBoneTypes.MISC,listOf("mWing2Right"),"-0.105 0.181 -0.099","",true),
        FSPoserJoint("mWing2Right","mWing2Left",EBoneTypes.MISC,listOf("mWing3Right"),"-0.169 0.067 -0.168","",true),
        FSPoserJoint("mWing3Right","mWing3Left",EBoneTypes.MISC,listOf("mWing4Right","mWing4FanRight"),"-0.183 0.000 -0.181","",true),
        FSPoserJoint("mWing4Right","mWing4Left",EBoneTypes.MISC,emptyList(),"-0.173 0.000 -0.171","-0.132 0.000 -0.146",true),
        FSPoserJoint("mWing4FanRight","mWing4FanLeft",EBoneTypes.MISC,emptyList(),"-0.173 0.000 -0.171","-0.062 -0.159 -0.068",true),
        // misc body
        FSPoserJoint("mSkull","",EBoneTypes.MISC,emptyList(),"0.079 0.000 0.000","0.033 0.000 0.000"),
        FSPoserJoint("mSpine1","",EBoneTypes.MISC,listOf("mSpine2"),"0.084 0.000 0.000"),
        FSPoserJoint("mSpine2","",EBoneTypes.MISC,listOf("mTorso"),"-0.084 0.000 0.000"),
        FSPoserJoint("mSpine3","",EBoneTypes.MISC,listOf("mSpine4"),"0.205 -0.015 0.000"),
        FSPoserJoint("mSpine4","",EBoneTypes.MISC,listOf("mChest"),"-0.205 0.015 0.000"),
        // collision volumes
        FSPoserJoint("HEAD","",EBoneTypes.COL_VOLUMES,emptyList(),"0 0.07 0.02","0.000 0.100 0.000"),
        FSPoserJoint("NECK","",EBoneTypes.COL_VOLUMES,emptyList(),"0 0.02 0.0","0.000 0.080 0.000"),
        FSPoserJoint("L_CLAVICLE","R_CLAVICLE",EBoneTypes.COL_VOLUMES,emptyList(),"0 0.02 0.02","0.1 0.0 0.0"),
        FSPoserJoint("R_CLAVICLE","L_CLAVICLE",EBoneTypes.COL_VOLUMES,emptyList(),"0 0.02 0.02","-0.1 0.0 0.0",true),
        FSPoserJoint("CHEST","",EBoneTypes.COL_VOLUMES,emptyList(),"0 0.07 0.028","0.000 0.152 -0.096"),
        FSPoserJoint("LEFT_PEC","RIGHT_PEC",EBoneTypes.COL_VOLUMES,emptyList(),"0.082 0.042 0.119","0.000 -0.006 0.080"),
        FSPoserJoint("RIGHT_PEC","LEFT_PEC",EBoneTypes.COL_VOLUMES,emptyList(),"-0.082 0.042 0.119","0.000 -0.006 0.080",true),
        FSPoserJoint("UPPER_BACK","",EBoneTypes.COL_VOLUMES,emptyList(),"0.0 0.017 0.0","0.0 0.0 -0.100"),
        FSPoserJoint("LEFT_HANDLE","RIGHT_HANDLE",EBoneTypes.COL_VOLUMES,emptyList(),"0.10 0.058 0.0","0.100 0.000 0.000"),
        FSPoserJoint("RIGHT_HANDLE","LEFT_HANDLE",EBoneTypes.COL_VOLUMES,emptyList(),"-0.10 0.058 0.0","-0.100 0.000 0.000",true),
        FSPoserJoint("BELLY","",EBoneTypes.COL_VOLUMES,emptyList(),"0 0.04 0.028","0.000 0.094 0.028"),
        FSPoserJoint("PELVIS","",EBoneTypes.COL_VOLUMES,emptyList(),"0 -0.02 -0.01","0.000 0.095 0.030"),
        FSPoserJoint("BUTT","",EBoneTypes.COL_VOLUMES,emptyList(),"0 -0.1 -0.06","0.000 0.000 -0.100"),
        FSPoserJoint("L_UPPER_ARM","R_UPPER_ARM",EBoneTypes.COL_VOLUMES,emptyList(),"0.12 0.01 0.0","0.130 -0.003 0.000"),
        FSPoserJoint("R_UPPER_ARM","L_UPPER_ARM",EBoneTypes.COL_VOLUMES,emptyList(),"-0.12 0.01 0.0","-0.130 -0.003 0.000",true),
        FSPoserJoint("L_LOWER_ARM","R_LOWER_ARM",EBoneTypes.COL_VOLUMES,emptyList(),"0.1 0.0 0.0","0.100 -0.001 0.000"),
        FSPoserJoint("R_LOWER_ARM","L_LOWER_ARM",EBoneTypes.COL_VOLUMES,emptyList(),"-0.1 0.0 0.0","-0.100 -0.001 0.000",true),
        FSPoserJoint("L_HAND","R_HAND",EBoneTypes.COL_VOLUMES,emptyList(),"0.05 0.0 0.01","0.049 -0.001 0.005"),
        FSPoserJoint("R_HAND","L_HAND",EBoneTypes.COL_VOLUMES,emptyList(),"-0.05 0.0 0.01","-0.049 -0.001 0.005",true),
        FSPoserJoint("L_UPPER_LEG","R_UPPER_LEG",EBoneTypes.COL_VOLUMES,emptyList(),"-0.05 -0.22 -0.02","0.000 -0.200 0.000"),
        FSPoserJoint("R_UPPER_LEG","L_UPPER_LEG",EBoneTypes.COL_VOLUMES,emptyList(),"0.05 -0.22 -0.02","0.000 -0.200 0.000",true),
        FSPoserJoint("L_LOWER_LEG","R_LOWER_LEG",EBoneTypes.COL_VOLUMES,emptyList(),"0.0 -0.2 -0.02","0.000 -0.150 -0.010"),
        FSPoserJoint("R_LOWER_LEG","L_LOWER_LEG",EBoneTypes.COL_VOLUMES,emptyList(),"0.0 -0.2 -0.02","0.000 -0.150 -0.010",true),
        FSPoserJoint("L_FOOT","R_FOOT",EBoneTypes.COL_VOLUMES,emptyList(),"0.0 -0.041 0.077","0.000 -0.026 0.089"),
        FSPoserJoint("R_FOOT","L_FOOT",EBoneTypes.COL_VOLUMES,emptyList(),"0.0 -0.041 0.077","0.000 -0.026 0.089",true),
    )

    private val posingState = FSPoseState()

    companion object {
        private val avatarIdToRegisteredAnimationId: MutableMap<UUID, UUID> = mutableMapOf()
    }

    fun getPoserJointByName(name: String): FSPoserJoint? =
        poserJoints.firstOrNull { it.jointName().equals(name, ignoreCase = true) }

    fun getPoserJointByNumber(avatar: Any?, jointNumber: Int): FSPoserJoint? {
        avatar ?: return null
        val posingMotion = getPosingMotion(avatar) ?: return null
        val parentJoint = getJointPoseByJointNumber(posingMotion, jointNumber) ?: return null
        return getPoserJointByName(parentJoint.jointName)
    }

    fun tryGetJointNumber(avatar: Any?, poserJoint: FSPoserJoint, jointNumberOut: IntArray): Boolean {
        avatar ?: return false
        val posingMotion = getPosingMotion(avatar) ?: return false
        val jointPose = getJointPoseByJointName(posingMotion, poserJoint.jointName()) ?: return false
        jointNumberOut[0] = jointPose.jointNumber
        return jointPose.jointNumber >= 0
    }

    fun isPosingAvatarJoint(avatar: Any?, joint: FSPoserJoint): Boolean {
        if (!isAvatarSafeToUse(avatar)) return false
        val posingMotion = getPosingMotion(avatar) ?: return false
        if (isMotionStopped(posingMotion)) return false
        val jointPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: return false
        return currentlyPosingJoint(posingMotion, jointPose)
    }

    fun hasJointBeenChanged(avatar: Any?, joint: FSPoserJoint): Boolean {
        if (!isAvatarSafeToUse(avatar)) return false
        val posingMotion = getPosingMotion(avatar) ?: return false
        if (isMotionStopped(posingMotion)) return false
        val jointPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: return false
        return jointPose.getJointModified()
    }

    fun setPosingAvatarJoint(avatar: Any?, joint: FSPoserJoint, shouldPose: Boolean) {
        if (!isAvatarSafeToUse(avatar)) return
        val arePosing = isPosingAvatarJoint(avatar, joint)
        if (arePosing == shouldPose) return
        val posingMotion = getPosingMotion(avatar) ?: return
        if (isMotionStopped(posingMotion)) return
        val jointPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: return
        if (shouldPose) addJointToState(posingMotion, jointPose)
        else removeJointFromState(posingMotion, jointPose)
    }

    fun resetJoint(avatar: Any?, joint: FSPoserJoint, style: EBoneDeflectionStyles) {
        if (!isAvatarSafeToUse(avatar)) return
        val posingMotion = getPosingMotion(avatar) ?: return
        if (isMotionStopped(posingMotion)) return
        val jointPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: return
        jointPose.resetJoint()
        if (style == EBoneDeflectionStyles.NONE || style == EBoneDeflectionStyles.DELTAMODE) return
        getJointPoseByJointName(posingMotion, joint.mirrorJointName())?.resetJoint()
    }

    fun undoLastJointChange(avatar: Any?, joint: FSPoserJoint, style: EBoneDeflectionStyles) {
        if (!isAvatarSafeToUse(avatar)) return
        val posingMotion = getPosingMotion(avatar) ?: return
        if (isMotionStopped(posingMotion)) return
        val jointPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: return

        val changeType = jointPose.undoLastChange()
        if (changeType == EPoserChangeType.POSER_CHANGE_ROTATION)
            undoOrRedoWorldLockedDescendants(joint, posingMotion, redo = false)

        if (changeType == EPoserChangeType.POSER_CHANGE_CHILDMOVE) {
            undoOrRedoRotatedParents(joint, posingMotion, redo = false)
            val parentPose = findParentJointPose(posingMotion, joint.jointName())
            if (parentPose != null) {
                val grandParentPose = findParentJointPose(posingMotion, parentPose.jointName)
                if (grandParentPose != null) {
                    val grandParentPoserJoint = getPoserJointByName(grandParentPose.jointName)
                    if (grandParentPoserJoint != null)
                        undoOrRedoWorldLockedDescendants(grandParentPoserJoint, posingMotion, redo = false)
                }
            }
        }

        if (style == EBoneDeflectionStyles.NONE || style == EBoneDeflectionStyles.DELTAMODE) return
        val oppositeJointPose = getJointPoseByJointName(posingMotion, joint.mirrorJointName()) ?: return
        val oppChangeType = oppositeJointPose.undoLastChange()
        if (oppChangeType != EPoserChangeType.POSER_CHANGE_ROTATION) return
        val oppositePoserJoint = getPoserJointByName(joint.mirrorJointName())
        if (oppositePoserJoint != null)
            undoOrRedoWorldLockedDescendants(oppositePoserJoint, posingMotion, redo = false)
    }

    fun canRedoOrUndoJointChange(avatar: Any?, joint: FSPoserJoint, canUndo: Boolean = false): Boolean {
        if (!isAvatarSafeToUse(avatar)) return false
        val posingMotion = getPosingMotion(avatar) ?: return false
        if (isMotionStopped(posingMotion)) return false
        val jointPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: return false
        return if (canUndo) jointPose.canPerformUndo() else jointPose.canPerformRedo()
    }

    fun redoLastJointChange(avatar: Any?, joint: FSPoserJoint, style: EBoneDeflectionStyles) {
        if (!isAvatarSafeToUse(avatar)) return
        val posingMotion = getPosingMotion(avatar) ?: return
        if (isMotionStopped(posingMotion)) return
        val jointPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: return

        val changeType = jointPose.redoLastChange()
        undoOrRedoWorldLockedDescendants(joint, posingMotion, redo = true)
        if (changeType == EPoserChangeType.POSER_CHANGE_CHILDMOVE)
            undoOrRedoRotatedParents(joint, posingMotion, redo = true)

        if (style == EBoneDeflectionStyles.NONE || style == EBoneDeflectionStyles.DELTAMODE) return
        val oppositeJointPose = getJointPoseByJointName(posingMotion, joint.mirrorJointName()) ?: return
        oppositeJointPose.redoLastChange()
        val oppositePoserJoint = getPoserJointByName(joint.mirrorJointName())
        if (oppositePoserJoint != null)
            undoOrRedoWorldLockedDescendants(oppositePoserJoint, posingMotion, redo = true)
    }

    fun getJointPosition(avatar: Any?, joint: FSPoserJoint): LLVector3 {
        if (!isAvatarSafeToUse(avatar)) return LLVector3.ZERO
        val posingMotion = getPosingMotion(avatar) ?: return LLVector3.ZERO
        return getJointPoseByJointName(posingMotion, joint.jointName())?.getPublicPosition() ?: LLVector3.ZERO
    }

    fun setJointPosition(avatar: Any?, joint: FSPoserJoint?, position: LLVector3, frame: EPoserReferenceFrame, style: EBoneDeflectionStyles) {
        if (!isAvatarSafeToUse(avatar) || joint == null) return
        if (joint.jointName().isEmpty()) return
        val posingMotion = getPosingMotion(avatar) ?: return
        val jointPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: return

        val currentPos = jointPose.getPublicPosition()
        val delta = currentPos - position

        when (style) {
            EBoneDeflectionStyles.MIRROR, EBoneDeflectionStyles.MIRROR_DELTA,
            EBoneDeflectionStyles.SYMPATHETIC_DELTA, EBoneDeflectionStyles.SYMPATHETIC ->
                jointPose.setPublicPosition(currentPos - delta)
            else -> {
                jointPose.setPublicPosition(currentPos - delta)
                return
            }
        }

        val oppositeJointPose = getJointPoseByJointName(posingMotion, joint.mirrorJointName()) ?: return
        val oppPos = oppositeJointPose.getPublicPosition()
        when (style) {
            EBoneDeflectionStyles.MIRROR, EBoneDeflectionStyles.MIRROR_DELTA ->
                oppositeJointPose.setPublicPosition(oppPos + delta)
            EBoneDeflectionStyles.SYMPATHETIC_DELTA, EBoneDeflectionStyles.SYMPATHETIC ->
                oppositeJointPose.setPublicPosition(oppPos - delta)
            else -> {}
        }
    }

    fun getJointRotation(avatar: Any?, joint: FSPoserJoint, translation: EBoneAxisTranslation, negation: Int): LLVector3 {
        if (!isAvatarSafeToUse(avatar)) return LLVector3.ZERO
        val posingMotion = getPosingMotion(avatar) ?: return LLVector3.ZERO
        val jointPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: return LLVector3.ZERO
        return translateRotationFromQuaternion(jointPose, translation, negation, jointPose.getPublicRotation())
    }

    fun getJointExportRotation(avatar: Any?, joint: FSPoserJoint, lockWholeAvatar: Boolean): LLVector3 {
        val rotation = getJointRotation(avatar, joint, EBoneAxisTranslation.SWAP_NOTHING, EBoneAxisNegation.NEGATE_NOTHING.bits)
        if (exportRotationWillLockJoint(avatar, joint)) return rotation

        if (!isAvatarSafeToUse(avatar)) return LLVector3.ZERO
        val posingMotion = getPosingMotion(avatar) ?: return LLVector3.ZERO
        val jointPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: return LLVector3.ZERO
        if (!jointPose.userHasSetBaseRotationToZero()) return LLVector3.ZERO

        if (lockWholeAvatar && joint.boneType() == EBoneTypes.WHOLEAVATAR)
            return LLVector3(Math.toRadians(0.295).toFloat(), 0f, 0f)

        val minimumRotation = Math.toRadians(0.65).toFloat() / maxOf(getChildJointDepth(joint, 0).toFloat() * 0.33f, 1f)
        return LLVector3(minimumRotation, 0f, 0f)
    }

    fun getManipGimbalRotation(avatar: Any?, joint: FSPoserJoint?, frame: EPoserReferenceFrame): LLQuaternion {
        val globalRot = LLQuaternion(-1f, 0f, 0f, 0f)
        if (frame == EPoserReferenceFrame.POSER_FRAME_WORLD) return globalRot
        joint ?: return globalRot
        if (!isAvatarSafeToUse(avatar)) return globalRot

        if (frame == EPoserReferenceFrame.POSER_FRAME_AVATAR) {
            return getPelvisWorldRotation(avatar) ?: globalRot
        }

        val posingMotion = getPosingMotion(avatar) ?: return globalRot
        val jointPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: return globalRot
        val llJoint = getUnderlyingJoint(jointPose) ?: return globalRot

        return if (frame == EPoserReferenceFrame.POSER_FRAME_BONE)
            llJoint.getWorldRotation()
        else
            getQuaternionFromWorldVector(llJoint.getWorldPosition() - getCameraPositionAgent())
    }

    fun setJointRotation(
        avatar: Any?, joint: FSPoserJoint?, absRotation: LLVector3, deltaRotation: LLVector3,
        style: EBoneDeflectionStyles, frame: EPoserReferenceFrame, translation: EBoneAxisTranslation,
        negation: Int, resetBaseRotationToZero: Boolean, rotationStyle: ERotationStyle
    ) {
        if (!isAvatarSafeToUse(avatar) || joint == null) return
        val posingMotion = getPosingMotion(avatar) ?: return
        val jointPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: return

        val translationRequiresDelta = frame != EPoserReferenceFrame.POSER_FRAME_BONE
        val absRot = translateRotationToQuaternion(avatar, jointPose, frame, translation, negation, absRotation)
        val deltaRot = translateRotationToQuaternion(avatar, jointPose, frame, translation, negation, deltaRotation)

        when (style) {
            EBoneDeflectionStyles.SYMPATHETIC, EBoneDeflectionStyles.MIRROR -> {
                if (rotationStyle == ERotationStyle.DELTAIC_ROT || translationRequiresDelta)
                    jointPose.setPublicRotation(resetBaseRotationToZero, true, EPoserChangeType.POSER_CHANGE_DEFAULT, deltaRot * jointPose.getPublicRotation())
                else
                    jointPose.setPublicRotation(resetBaseRotationToZero, true, EPoserChangeType.POSER_CHANGE_DEFAULT, absRot)
            }
            EBoneDeflectionStyles.SYMPATHETIC_DELTA, EBoneDeflectionStyles.MIRROR_DELTA ->
                jointPose.setPublicRotation(resetBaseRotationToZero, true, EPoserChangeType.POSER_CHANGE_DEFAULT, deltaRot * jointPose.getPublicRotation())
            EBoneDeflectionStyles.DELTAMODE -> {
                jointPose.setPublicRotation(resetBaseRotationToZero, true, EPoserChangeType.POSER_CHANGE_DEFAULT, deltaRot * jointPose.getPublicRotation())
                deRotateWorldLockedDescendants(joint, posingMotion, deltaRot)
                return
            }
            else -> {
                if (rotationStyle == ERotationStyle.DELTAIC_ROT || translationRequiresDelta)
                    jointPose.setPublicRotation(resetBaseRotationToZero, true, EPoserChangeType.POSER_CHANGE_DEFAULT, deltaRot * jointPose.getPublicRotation())
                else
                    jointPose.setPublicRotation(resetBaseRotationToZero, true, EPoserChangeType.POSER_CHANGE_DEFAULT, absRot)
                deRotateWorldLockedDescendants(joint, posingMotion, deltaRot)
                return
            }
        }

        deRotateWorldLockedDescendants(joint, posingMotion, deltaRot)

        val oppositePoserJoint = getPoserJointByName(joint.mirrorJointName())
        val oppositeJointPose = getJointPoseByJointName(posingMotion, joint.mirrorJointName()) ?: return
        val mirroredRot = LLQuaternion(-deltaRot.x, deltaRot.y, -deltaRot.z, deltaRot.w)

        when (style) {
            EBoneDeflectionStyles.SYMPATHETIC -> {
                oppositeJointPose.cloneRotationFrom(jointPose)
                oppositePoserJoint?.let { deRotateWorldLockedDescendants(it, posingMotion, deltaRot) }
            }
            EBoneDeflectionStyles.SYMPATHETIC_DELTA -> {
                oppositeJointPose.setPublicRotation(resetBaseRotationToZero, true, EPoserChangeType.POSER_CHANGE_DEFAULT, deltaRot * oppositeJointPose.getPublicRotation())
                oppositePoserJoint?.let { deRotateWorldLockedDescendants(it, posingMotion, deltaRot) }
            }
            EBoneDeflectionStyles.MIRROR -> {
                oppositeJointPose.mirrorRotationFrom(jointPose)
                oppositePoserJoint?.let { deRotateWorldLockedDescendants(it, posingMotion, mirroredRot) }
            }
            EBoneDeflectionStyles.MIRROR_DELTA -> {
                oppositeJointPose.setPublicRotation(resetBaseRotationToZero, true, EPoserChangeType.POSER_CHANGE_DEFAULT, mirroredRot * oppositeJointPose.getPublicRotation())
                oppositePoserJoint?.let { deRotateWorldLockedDescendants(it, posingMotion, mirroredRot) }
            }
            else -> {}
        }
    }

    fun getJointScale(avatar: Any?, joint: FSPoserJoint): LLVector3 {
        if (!isAvatarSafeToUse(avatar)) return LLVector3.ZERO
        val posingMotion = getPosingMotion(avatar) ?: return LLVector3.ZERO
        return getJointPoseByJointName(posingMotion, joint.jointName())?.getPublicScale() ?: LLVector3.ZERO
    }

    fun setJointScale(avatar: Any?, joint: FSPoserJoint?, scale: LLVector3, frame: EPoserReferenceFrame, style: EBoneDeflectionStyles) {
        if (!isAvatarSafeToUse(avatar) || joint == null) return
        if (joint.jointName().isEmpty()) return
        val posingMotion = getPosingMotion(avatar) ?: return
        val jointPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: return

        val current = jointPose.getPublicScale()
        val delta = current - scale

        when (style) {
            EBoneDeflectionStyles.MIRROR, EBoneDeflectionStyles.MIRROR_DELTA,
            EBoneDeflectionStyles.SYMPATHETIC_DELTA, EBoneDeflectionStyles.SYMPATHETIC ->
                jointPose.setPublicScale(current - delta)
            else -> {
                jointPose.setPublicScale(current - delta)
                return
            }
        }

        val oppositeJointPose = getJointPoseByJointName(posingMotion, joint.mirrorJointName()) ?: return
        val oppScale = oppositeJointPose.getPublicScale()
        when (style) {
            EBoneDeflectionStyles.MIRROR, EBoneDeflectionStyles.MIRROR_DELTA ->
                oppositeJointPose.setPublicScale(oppScale + delta)
            EBoneDeflectionStyles.SYMPATHETIC_DELTA, EBoneDeflectionStyles.SYMPATHETIC ->
                oppositeJointPose.setPublicScale(oppScale - delta)
            else -> {}
        }
    }

    fun reflectJoint(avatar: Any?, joint: FSPoserJoint?) {
        if (!isAvatarSafeToUse(avatar) || joint == null) return
        val posingMotion = getPosingMotion(avatar) ?: return
        val jointPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: return
        jointPose.reflectRotation()
        val oppPose = getJointPoseByJointName(posingMotion, joint.mirrorJointName())
        if (oppPose != null) {
            oppPose.reflectRotation()
            jointPose.swapRotationWith(oppPose)
        }
    }

    fun flipEntirePose(avatar: Any?) {
        if (!isAvatarSafeToUse(avatar)) return
        for (joint in poserJoints) {
            if (joint.dontFlipOnMirror()) continue
            if (!isPosingAvatarJoint(avatar, joint)) continue
            val opposite = getPoserJointByName(joint.mirrorJointName())
            if (opposite != null && !isPosingAvatarJoint(avatar, opposite)) continue
            reflectJoint(avatar, joint)
        }
    }

    fun symmetrizeLeftToRightOrRightToLeft(avatar: Any?, rightToLeft: Boolean) {
        if (!isAvatarSafeToUse(avatar)) return
        val posingMotion = getPosingMotion(avatar) ?: return
        for (joint in poserJoints) {
            if (!joint.dontFlipOnMirror()) continue
            if (!isPosingAvatarJoint(avatar, joint)) continue
            val opposite = getPoserJointByName(joint.mirrorJointName()) ?: continue
            if (!isPosingAvatarJoint(avatar, opposite)) continue
            val rightPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: continue
            val leftPose = getJointPoseByJointName(posingMotion, opposite.jointName()) ?: continue
            if (rightToLeft) leftPose.mirrorRotationFrom(rightPose)
            else rightPose.mirrorRotationFrom(leftPose)
        }
    }

    fun recaptureJoint(avatar: Any?, joint: FSPoserJoint) {
        if (!isAvatarSafeToUse(avatar)) return
        val posingMotion = getPosingMotion(avatar) ?: return
        getJointPoseByJointName(posingMotion, joint.jointName())?.recaptureJoint()
        setPosingAvatarJoint(avatar, joint, shouldPose = true)
    }

    fun updateJointFromManip(
        avatar: Any?, joint: FSPoserJoint?, resetBaseRotationToZero: Boolean,
        style: EBoneDeflectionStyles, frame: EPoserReferenceFrame,
        rotation: LLQuaternion, position: LLVector3, scale: LLVector3
    ) {
        if (!position.isZero())
            updateJointPositionFromManip(avatar, joint, resetBaseRotationToZero, style, position)
        else
            updateJointRotationFromManip(avatar, joint, resetBaseRotationToZero, style, frame, rotation)
    }

    fun setAllAvatarStartingRotationsToZero(avatar: Any?) {
        if (!isAvatarSafeToUse(avatar)) return
        val posingMotion = getPosingMotion(avatar) ?: return
        setAllRotationsToZeroAndClearUndo(posingMotion)
        posingState.purgeMotionStates(avatar)

        for (joint in poserJoints) {
            val boneType = joint.boneType()
            if (boneType == EBoneTypes.BODY || boneType == EBoneTypes.WHOLEAVATAR) continue
            val jointPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: continue
            setJointBvhLock(posingMotion, jointPose, false)
        }
    }

    fun userSetBaseRotationToZero(avatar: Any?, joint: FSPoserJoint): Boolean {
        if (!isAvatarSafeToUse(avatar)) return false
        val posingMotion = getPosingMotion(avatar) ?: return false
        return getJointPoseByJointName(posingMotion, joint.jointName())?.userHasSetBaseRotationToZero() ?: false
    }

    fun exportRotationWillLockJoint(avatar: Any?, joint: FSPoserJoint): Boolean {
        val rotationKeyframeThreshold = 0.01f
        if (!isAvatarSafeToUse(avatar)) return false
        val posingMotion = getPosingMotion(avatar) ?: return false
        val jointPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: return false
        if (!jointPose.userHasSetBaseRotationToZero()) return false

        val rotThreshold = rotationKeyframeThreshold / maxOf(getChildJointDepth(joint, 0).toFloat() * 0.33f, 1f)
        val rotToExport = jointPose.getPublicRotation()
        return false
    }

    fun getRotationIsWorldLocked(avatar: Any?, joint: FSPoserJoint): Boolean {
        if (!isAvatarSafeToUse(avatar)) return false
        val posingMotion = getPosingMotion(avatar) ?: return false
        return getJointPoseByJointName(posingMotion, joint.jointName())?.getWorldRotationLockState() ?: false
    }

    fun setRotationIsWorldLocked(avatar: Any?, joint: FSPoserJoint, newState: Boolean) {
        if (!isAvatarSafeToUse(avatar)) return
        val posingMotion = getPosingMotion(avatar) ?: return
        getJointPoseByJointName(posingMotion, joint.jointName())?.setWorldRotationLockState(newState)
    }

    fun getRotationIsMirrored(avatar: Any?, joint: FSPoserJoint): Boolean {
        if (!isAvatarSafeToUse(avatar)) return false
        val posingMotion = getPosingMotion(avatar) ?: return false
        return getJointPoseByJointName(posingMotion, joint.jointName())?.getRotationMirrorState() ?: false
    }

    fun setRotationIsMirrored(avatar: Any?, joint: FSPoserJoint, newState: Boolean) {
        if (!isAvatarSafeToUse(avatar)) return
        val posingMotion = getPosingMotion(avatar) ?: return
        getJointPoseByJointName(posingMotion, joint.jointName())?.setRotationMirrorState(newState)
    }

    fun allBaseRotationsAreZero(avatar: Any?): Boolean {
        if (!isAvatarSafeToUse(avatar)) return false
        val posingMotion = getPosingMotion(avatar) ?: return false
        return allStartingRotationsAreZero(posingMotion)
    }

    fun tryGetJointSaveVectors(
        avatar: Any?, joint: FSPoserJoint,
        rot: LLVector3, pos: LLVector3, scale: LLVector3,
        baseRotationIsZero: BooleanArray, userSetBaseRotZero: BooleanArray
    ): Boolean {
        if (!isAvatarSafeToUse(avatar)) return false
        val posingMotion = getPosingMotion(avatar) ?: return false
        val jointPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: return false

        val (rx, ry, rz) = jointPose.getPublicRotation().getEulerAngles()
        rot.x = rx; rot.y = ry; rot.z = rz
        val p = jointPose.getPublicPosition()
        pos.x = p.x; pos.y = p.y; pos.z = p.z
        val s = jointPose.getPublicScale()
        scale.x = s.x; scale.y = s.y; scale.z = s.z
        baseRotationIsZero[0] = jointPose.isBaseRotationZero()
        userSetBaseRotZero[0] = jointPose.userHasSetBaseRotationToZero()
        return true
    }

    fun loadJointRotation(avatar: Any?, joint: FSPoserJoint?, setBaseToZero: Boolean, userSetBaseToZero: Boolean, rotation: LLVector3) {
        if (!isAvatarSafeToUse(avatar) || joint == null) return
        val posingMotion = getPosingMotion(avatar) ?: return
        val jointPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: return
        jointPose.purgeUndoQueue()
        val rot = translateRotationToQuaternion(avatar, jointPose, EPoserReferenceFrame.POSER_FRAME_BONE, EBoneAxisTranslation.SWAP_NOTHING, EBoneAxisNegation.NEGATE_NOTHING.bits, rotation)
        jointPose.setPublicRotation(setBaseToZero, userSetBaseToZero, EPoserChangeType.POSER_CHANGE_DEFAULT, rot)
    }

    fun loadJointPosition(avatar: Any?, joint: FSPoserJoint?, loadPositionAsDelta: Boolean, position: LLVector3) {
        if (!isAvatarSafeToUse(avatar) || joint == null) return
        val posingMotion = getPosingMotion(avatar) ?: return
        val jointPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: return
        if (loadPositionAsDelta) jointPose.setPublicPosition(position)
        else {
            jointPose.setJointPriority(JointPriority.LOW_PRIORITY)
            jointPose.setBasePosition(position, JointPriority.LOW_PRIORITY)
        }
    }

    fun loadJointScale(avatar: Any?, joint: FSPoserJoint?, loadScaleAsDelta: Boolean, scale: LLVector3) {
        if (!isAvatarSafeToUse(avatar) || joint == null) return
        val posingMotion = getPosingMotion(avatar) ?: return
        val jointPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: return
        if (loadScaleAsDelta) jointPose.setPublicScale(scale)
        else {
            jointPose.setJointPriority(JointPriority.LOW_PRIORITY)
            jointPose.setBaseScale(scale, JointPriority.LOW_PRIORITY)
            jointPose.setPublicScale(LLVector3.ZERO)
        }
    }

    fun loadPosingState(avatar: Any?, ignoreOwnership: Boolean, pose: Map<String, Any>): Boolean {
        if (!isAvatarSafeToUse(avatar)) return false
        posingState.purgeMotionStates(avatar)
        posingState.restoreMotionStates(avatar, ignoreOwnership, pose)
        return applyStatesToPosingMotion(avatar)
    }

    fun applyStatesToPosingMotion(avatar: Any?): Boolean {
        if (!isAvatarSafeToUse(avatar)) return false
        val posingMotion = getPosingMotion(avatar) ?: return false
        val success = posingState.applyMotionStatesToPosingMotion(avatar, posingMotion)
        if (success) applyJointMirrorToBaseRotations(posingMotion)
        return success
    }

    fun savePosingState(avatar: Any?, ignoreOwnership: Boolean, saveRecord: MutableMap<String, Any>) {
        posingState.writeMotionStates(avatar, ignoreOwnership, saveRecord)
    }

    fun updatePosingState(avatar: Any?, jointsRecaptured: List<FSPoserJoint>) {
        avatar ?: return
        val posingMotion = getPosingMotion(avatar) ?: return
        val jointNumbers = jointsRecaptured.mapNotNull { item ->
            getJointPoseByJointName(posingMotion, item.jointName())?.jointNumber
        }
        posingState.updateMotionStates(avatar, posingMotion, jointNumbers.toMutableList())
    }

    fun applyJointMirrorToBaseRotations(posingMotion: Any) {
        for (joint in poserJoints) {
            val jointPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: continue
            if (!jointPose.getRotationMirrorState()) continue
            if (joint.dontFlipOnMirror()) continue
            jointPose.reflectBaseRotation()
            val oppPose = getJointPoseByJointName(posingMotion, joint.mirrorJointName()) ?: continue
            oppPose.reflectBaseRotation()
            jointPose.swapBaseRotationWith(oppPose)
        }
    }

    fun tryPosingAvatar(avatar: Any?): Boolean {
        if (!isAvatarSafeToUse(avatar)) return false
        val posingMotion = findOrCreatePosingMotion(avatar) ?: return false
        if (!isMotionStopped(posingMotion)) return false
        if (isSelf(avatar)) stopFidget()
        posingState.captureMotionStates(avatar)
        startDefaultMotions(avatar)
        startMotion(avatar, getMotionId(posingMotion))
        return true
    }

    fun stopPosingAvatar(avatar: Any?) {
        if (avatar == null || isAvatarDead(avatar)) return
        val posingMotion = getPosingMotion(avatar) ?: return
        posingState.purgeMotionStates(avatar)
        stopMotion(avatar, getMotionId(posingMotion))
    }

    fun isPosingAvatar(avatar: Any?): Boolean {
        if (!isAvatarSafeToUse(avatar)) return false
        val posingMotion = getPosingMotion(avatar) ?: return false
        return !isMotionStopped(posingMotion)
    }

    private fun updateJointRotationFromManip(
        avatar: Any?, joint: FSPoserJoint?, resetBaseRotationToZero: Boolean,
        style: EBoneDeflectionStyles, frame: EPoserReferenceFrame, rotation: LLQuaternion
    ) {
        if (!isAvatarSafeToUse(avatar) || joint == null) return
        val posingMotion = getPosingMotion(avatar) ?: return
        val jointPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: return

        val framedRotation = changeToRotationFrame(avatar, rotation, frame, jointPose)
        jointPose.setPublicRotation(resetBaseRotationToZero, true, EPoserChangeType.POSER_CHANGE_ROTATION, framedRotation * jointPose.getPublicRotation())
        deRotateWorldLockedDescendants(joint, posingMotion, framedRotation)

        if (style == EBoneDeflectionStyles.NONE || style == EBoneDeflectionStyles.DELTAMODE) return

        val oppositePoserJoint = getPoserJointByName(joint.mirrorJointName())
        val oppositeJointPose = getJointPoseByJointName(posingMotion, joint.mirrorJointName()) ?: return
        val mirroredRot = LLQuaternion(-framedRotation.x, framedRotation.y, -framedRotation.z, framedRotation.w)

        when (style) {
            EBoneDeflectionStyles.SYMPATHETIC, EBoneDeflectionStyles.SYMPATHETIC_DELTA -> {
                oppositeJointPose.setPublicRotation(resetBaseRotationToZero, true, EPoserChangeType.POSER_CHANGE_ROTATION, framedRotation * oppositeJointPose.getPublicRotation())
                oppositePoserJoint?.let { deRotateWorldLockedDescendants(it, posingMotion, framedRotation) }
            }
            EBoneDeflectionStyles.MIRROR, EBoneDeflectionStyles.MIRROR_DELTA -> {
                oppositeJointPose.setPublicRotation(resetBaseRotationToZero, true, EPoserChangeType.POSER_CHANGE_ROTATION, mirroredRot * oppositeJointPose.getPublicRotation())
                oppositePoserJoint?.let { deRotateWorldLockedDescendants(it, posingMotion, mirroredRot) }
            }
            else -> {}
        }
    }

    private fun updateJointPositionFromManip(
        avatar: Any?, joint: FSPoserJoint?, resetBaseRotationToZero: Boolean,
        style: EBoneDeflectionStyles, position: LLVector3
    ) {
        // no-op
    }

    private fun translateRotationToQuaternion(
        avatar: Any?, joint: FSJointPose, frame: EPoserReferenceFrame,
        translation: EBoneAxisTranslation, negation: Int, rotation: LLVector3
    ): LLQuaternion {
        return LLQuaternion(0f, 0f, 0f, 1f)
    }

    private fun changeToRotationFrame(avatar: Any?, rotation: LLQuaternion, frame: EPoserReferenceFrame, joint: FSJointPose): LLQuaternion {
        return LLQuaternion(0f, 0f, 0f, 1f)
    }

    private fun translateRotationFromQuaternion(joint: FSJointPose, translation: EBoneAxisTranslation, negation: Int, rotation: LLQuaternion): LLVector3 {
        return LLVector3.ZERO
    }

    private fun getChildJointDepth(joint: FSPoserJoint, depth: Int): Int {
        if (joint.bvhChildren().isEmpty()) return depth
        var maxDepth = depth + 1
        for (childName in joint.bvhChildren()) {
            val next = getPoserJointByName(childName) ?: continue
            maxDepth = maxOf(maxDepth, getChildJointDepth(next, maxDepth))
        }
        return maxDepth
    }

    private fun deRotateWorldLockedDescendants(joint: FSPoserJoint, posingMotion: Any, rotationChange: LLQuaternion) {
        if (joint.bvhChildren().isEmpty()) return
        val parentJointPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: return
        val pJoint = getUnderlyingJoint(parentJointPose) ?: return
        for (childName in joint.bvhChildren()) {
            val next = getPoserJointByName(childName) ?: continue
            deRotateJointOrFirstLockedChild(next, posingMotion, pJoint.getWorldRotation(), rotationChange)
        }
    }

    private fun deRotateJointOrFirstLockedChild(joint: FSPoserJoint, posingMotion: Any, rotatedParentWorldRot: LLQuaternion, rotationChange: LLQuaternion) {
        val jointPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: return
        if (jointPose.getWorldRotationLockState()) {
            // no-op
            return
        }
        if (joint.bvhChildren().isEmpty()) return
        for (childName in joint.bvhChildren()) {
            val next = getPoserJointByName(childName) ?: continue
            deRotateJointOrFirstLockedChild(next, posingMotion, rotatedParentWorldRot, rotationChange)
        }
    }

    private fun undoOrRedoRotatedParents(joint: FSPoserJoint, posingMotion: Any, redo: Boolean) {
        val parentPose = findParentJointPose(posingMotion, joint.jointName()) ?: return
        if (redo) parentPose.redoLastChange() else parentPose.undoLastChange()
        val grandParentPose = findParentJointPose(posingMotion, parentPose.jointName) ?: return
        if (redo) grandParentPose.redoLastChange() else grandParentPose.undoLastChange()
    }

    private fun undoOrRedoWorldLockedDescendants(joint: FSPoserJoint, posingMotion: Any, redo: Boolean) {
        for (childName in joint.bvhChildren()) {
            val next = getPoserJointByName(childName) ?: continue
            undoOrRedoJointOrFirstLockedChild(next, posingMotion, redo)
        }
    }

    private fun undoOrRedoJointOrFirstLockedChild(joint: FSPoserJoint, posingMotion: Any, redo: Boolean) {
        val jointPose = getJointPoseByJointName(posingMotion, joint.jointName()) ?: return
        if (jointPose.getWorldRotationLockState()) {
            if (redo) jointPose.redoLastChange() else jointPose.undoLastChange()
            return
        }
        for (childName in joint.bvhChildren()) {
            val next = getPoserJointByName(childName) ?: continue
            undoOrRedoJointOrFirstLockedChild(next, posingMotion, redo)
        }
    }

    private fun findParentJointPose(posingMotion: Any, jointName: String): FSJointPose? {
        if (jointName.isEmpty()) return null
        for (joint in poserJoints) {
            if (jointName in joint.bvhChildren())
                return getJointPoseByJointName(posingMotion, joint.jointName())
        }
        return null
    }

    private fun getQuaternionFromWorldVector(worldVector: LLVector3): LLQuaternion {
        return LLQuaternion(0f, 0f, 0f, 1f)
    }

    private fun isAvatarSafeToUse(avatar: Any?): Boolean = false
    private fun isAvatarDead(avatar: Any): Boolean = false
    private fun isSelf(avatar: Any?): Boolean = false
    private fun stopFidget() { /* no-op */ }
    private fun startDefaultMotions(avatar: Any?) { /* no-op */ }
    private fun startMotion(avatar: Any?, motionId: UUID) { /* no-op */ }
    private fun stopMotion(avatar: Any?, motionId: UUID) { /* no-op */ }
    private fun getPosingMotion(avatar: Any?): Any? = null
    private fun findOrCreatePosingMotion(avatar: Any?): Any? = null
    private fun isMotionStopped(posingMotion: Any): Boolean = false
    private fun getMotionId(posingMotion: Any): UUID = UUID.randomUUID()
    private fun getJointPoseByJointName(posingMotion: Any, jointName: String): FSJointPose? = null
    private fun getJointPoseByJointNumber(posingMotion: Any, jointNumber: Int): FSJointPose? = null
    private fun currentlyPosingJoint(posingMotion: Any, jointPose: FSJointPose): Boolean = false
    private fun addJointToState(posingMotion: Any, jointPose: FSJointPose) { /* no-op */ }
    private fun removeJointFromState(posingMotion: Any, jointPose: FSJointPose) { /* no-op */ }
    private fun setAllRotationsToZeroAndClearUndo(posingMotion: Any) { /* no-op */ }
    private fun setJointBvhLock(posingMotion: Any, jointPose: FSJointPose, lock: Boolean) { /* no-op */ }
    private fun allStartingRotationsAreZero(posingMotion: Any): Boolean = false
    private fun getUnderlyingJoint(jointPose: FSJointPose): LLJoint? = null
    private fun getPelvisWorldRotation(avatar: Any?): LLQuaternion? = null
    private fun getCameraPositionAgent(): LLVector3 = LLVector3.ZERO
}
