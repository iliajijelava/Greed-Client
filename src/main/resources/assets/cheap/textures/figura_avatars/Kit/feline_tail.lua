--Hello!--

--requires--
local squapi = require("SquAPI")

--SquAPI Tail--
local myTail = {
	models.model.root.Body.tail.base,
    models.model.root.Body.tail.base.base2,
    models.model.root.Body.tail.base.base2.mid1,
    models.model.root.Body.tail.base.base2.mid1.mid1a,
    models.model.root.Body.tail.base.base2.mid1.mid1a.mid2,
    models.model.root.Body.tail.base.base2.mid1.mid1a.mid2.mid2a,
    models.model.root.Body.tail.base.base2.mid1.mid1a.mid2.mid2a.tip,
    models.model.root.Body.tail.base.base2.mid1.mid1a.mid2.mid2a.tip.tip2,
    --tail segments go here
}
--replace each nil with the value/parmater you want to use, or leave as nil to use default values :)
--parenthesis are default values for reference
squapi.tail:new(myTail,
    10,    --(15) idleXMovement
    nil,    --(5) idleYMovement
    0.75,    --(1.2) idleXSpeed
    nil,    --(2) idleYSpeed
    nil,    --(2) bendStrength
    nil,    --(0) velocityPush
    nil,    --(0) initialMovementOffset
    0.5,    --(1) offsetBetweenSegments
    .001,    --(.005) stiffness
    nil,    --(.9) bounce
    nil,    --(90) flyingOffset
    nil,    --(-90) downLimit
    nil     --(45) upLimit
)
