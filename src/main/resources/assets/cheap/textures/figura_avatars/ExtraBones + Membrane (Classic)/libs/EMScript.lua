local membrane = require("libs.membrane")

membrane:define(models.model_blend.root.Membranes.Membrane1, {
    models.model_blend.root.LeftShoulder.LeftArm.LFURight,
    models.model_blend.root.LeftShoulder.LeftArm.LArmLower.LFLRight,
    models.model_blend.root.LeftShoulder.LeftArm.LArmLower.LFLLeft,
    models.model_blend.root.LeftShoulder.LeftArm.LFULeft
})

membrane:define(models.model_blend.root.Membranes.Membrane2, {
    models.model_blend.root.LeftShoulder.LeftArm.LBURight,
    models.model_blend.root.LeftShoulder.LeftArm.LArmLower.LBLRight,
    models.model_blend.root.LeftShoulder.LeftArm.LArmLower.LBLLeft,
    models.model_blend.root.LeftShoulder.LeftArm.LBULeft
})

membrane:define(models.model_blend.root.Membranes.Membrane3, {
    models.model_blend.root.LeftShoulder.LeftArm.LArmLower.LBLLeft,
    models.model_blend.root.LeftShoulder.LeftArm.LBULeft,
    models.model_blend.root.LeftShoulder.LeftArm.LFULeft,
    models.model_blend.root.LeftShoulder.LeftArm.LArmLower.LFLLeft
})

membrane:define(models.model_blend.root.Membranes.Membrane4, {
    models.model_blend.root.LeftShoulder.LeftArm.LArmLower.LBLRight,
    models.model_blend.root.LeftShoulder.LeftArm.LBURight,
    models.model_blend.root.LeftShoulder.LeftArm.LFURight,
    models.model_blend.root.LeftShoulder.LeftArm.LArmLower.LFLRight
})

membrane:define(models.model_blend.root.Membranes.Membrane5, {
    models.model_blend.root.RightShoulder.RightArm.RFURight,
    models.model_blend.root.RightShoulder.RightArm.RArmLower.RFLRight,
    models.model_blend.root.RightShoulder.RightArm.RArmLower.RFLLeft,
    models.model_blend.root.RightShoulder.RightArm.RFULeft
})

membrane:define(models.model_blend.root.Membranes.Membrane6, {
    models.model_blend.root.RightShoulder.RightArm.RBURight,
    models.model_blend.root.RightShoulder.RightArm.RArmLower.RBLRight,
    models.model_blend.root.RightShoulder.RightArm.RArmLower.RBLLeft,
    models.model_blend.root.RightShoulder.RightArm.RBULeft
})

membrane:define(models.model_blend.root.Membranes.Membrane7, {
    models.model_blend.root.RightShoulder.RightArm.RArmLower.RBLLeft,
    models.model_blend.root.RightShoulder.RightArm.RBULeft,
    models.model_blend.root.RightShoulder.RightArm.RFULeft,
    models.model_blend.root.RightShoulder.RightArm.RArmLower.RFLLeft
})

membrane:define(models.model_blend.root.Membranes.Membrane8, {
    models.model_blend.root.RightShoulder.RightArm.RArmLower.RBLRight,
    models.model_blend.root.RightShoulder.RightArm.RBURight,
    models.model_blend.root.RightShoulder.RightArm.RFURight,
    models.model_blend.root.RightShoulder.RightArm.RArmLower.RFLRight
})

membrane:define(models.model_blend.root.Membranes.Membrane9, {
    models.model_blend.root.LeftLeg.LFURightLeg,
    models.model_blend.root.LeftLeg.LLegLower.LFLRightLeg,
    models.model_blend.root.LeftLeg.LLegLower.LFLLeftLeg,
    models.model_blend.root.LeftLeg.LFULeftLeg
})

membrane:define(models.model_blend.root.Membranes.Membrane10, {
    models.model_blend.root.LeftLeg.LBURightLeg,
    models.model_blend.root.LeftLeg.LLegLower.LBLRightLeg,
    models.model_blend.root.LeftLeg.LLegLower.LBLLeftLeg,
    models.model_blend.root.LeftLeg.LBULeftLeg
})

membrane:define(models.model_blend.root.Membranes.Membrane11, {
    models.model_blend.root.LeftLeg.LLegLower.LBLLeftLeg,
    models.model_blend.root.LeftLeg.LBULeftLeg,
    models.model_blend.root.LeftLeg.LFULeftLeg,
    models.model_blend.root.LeftLeg.LLegLower.LFLLeftLeg
})

membrane:define(models.model_blend.root.Membranes.Membrane12, {
    models.model_blend.root.LeftLeg.LLegLower.LBLRightLeg,
    models.model_blend.root.LeftLeg.LBURightLeg,
    models.model_blend.root.LeftLeg.LFURightLeg,
    models.model_blend.root.LeftLeg.LLegLower.LFLRightLeg
})

membrane:define(models.model_blend.root.Membranes.Membrane13, {
    models.model_blend.root.RightLeg.RFURightLeg,
    models.model_blend.root.RightLeg.RLegLower.RFLRightLeg,
    models.model_blend.root.RightLeg.RLegLower.RFLLeftLeg,
    models.model_blend.root.RightLeg.RFULeftLeg
})

membrane:define(models.model_blend.root.Membranes.Membrane14, {
    models.model_blend.root.RightLeg.RBURightLeg,
    models.model_blend.root.RightLeg.RLegLower.RBLRightLeg,
    models.model_blend.root.RightLeg.RLegLower.RBLLeftLeg,
    models.model_blend.root.RightLeg.RBULeftLeg
})

membrane:define(models.model_blend.root.Membranes.Membrane15, {
    models.model_blend.root.RightLeg.RLegLower.RBLLeftLeg,
    models.model_blend.root.RightLeg.RBULeftLeg,
    models.model_blend.root.RightLeg.RFULeftLeg,
    models.model_blend.root.RightLeg.RLegLower.RFLLeftLeg
})

membrane:define(models.model_blend.root.Membranes.Membrane16, {
    models.model_blend.root.RightLeg.RLegLower.RBLRightLeg,
    models.model_blend.root.RightLeg.RBURightLeg,
    models.model_blend.root.RightLeg.RFURightLeg,
    models.model_blend.root.RightLeg.RLegLower.RFLRightLeg
})

membrane:define(models.model_blend.root.Membranes.Membrane17, {
    models.model_blend.root.Body.BFURight,
    models.model_blend.root.Body.chest.BFLRight,
    models.model_blend.root.Body.chest.BFLLeft,
    models.model_blend.root.Body.BFULeft
})

membrane:define(models.model_blend.root.Membranes.Membrane18, {
    models.model_blend.root.Body.BBURight,
    models.model_blend.root.Body.chest.BBLRight,
    models.model_blend.root.Body.chest.BBLLeft,
    models.model_blend.root.Body.BBULeft
})

membrane:define(models.model_blend.root.Membranes.Membrane19, {
    models.model_blend.root.Body.chest.BBLLeft,
    models.model_blend.root.Body.BBULeft,
    models.model_blend.root.Body.BFULeft,
    models.model_blend.root.Body.chest.BFLLeft
})

membrane:define(models.model_blend.root.Membranes.Membrane20, {
    models.model_blend.root.Body.chest.BBLRight,
    models.model_blend.root.Body.BBURight,
    models.model_blend.root.Body.BFURight,
    models.model_blend.root.Body.chest.BFLRight
}) 
