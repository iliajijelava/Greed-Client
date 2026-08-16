local PirOdd = require("PirateOddities")

MODELPATH = models.model

local breasts = PirOdd.BouncyPart(
    MODELPATH.root.Body.torso.boob,
    vec(0,0.08,0),
                                  vec(1.0,1.2,0.5)
)

breasts.rotBounce.min = vec(-15,-10,-5)
breasts.rotBounce.max = vec(15,8,5)

breasts.posBounce.min = vec(0,-0.25,0)
breasts.posBounce.max = vec(0,0.25,0)

breasts.breastStuff = true
