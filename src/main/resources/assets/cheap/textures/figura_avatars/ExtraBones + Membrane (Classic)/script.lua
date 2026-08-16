-- Mirror @MagicLab --
local BoneInit = require("libs.extrabone_lib")
BoneInit({
    { models.model_blend.root.Neck, "body" },
    { models.model_blend.root.Body.chest, "body" },
    { models.model_blend.root.LeftShoulder, "body" },
    { models.model_blend.root.RightShoulder, "body" },
    { models.model_blend.root.LeftShoulder.LeftArm.LArmLower, "leftArm" },
    { models.model_blend.root.RightShoulder.RightArm.RArmLower, "rightArm" },
    { models.model_blend.root.LeftLeg.LLegLower, "leftLeg" },
    { models.model_blend.root.RightLeg.RLegLower, "rightLeg" }
})

--hide vanilla model
vanilla_model.PLAYER:setVisible(false)



