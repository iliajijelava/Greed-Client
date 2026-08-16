-- Auto generated script file --

--hide vanilla armor model
vanilla_model.ARMOR:setVisible(false)

--hide vanilla cape model
vanilla_model.CAPE:setVisible(false)


--
require("GSAnimBlend")
local anims = require("JimmyAnims")
anims.excluBlendTime = 1.5
anims.incluBlendTime = 1.5
anims.autoBlend = true
anims(animations.LargeLamb)

--
vanilla_model.PLAYER:setVisible(false)
--
local squapi = require("SquAPI")
--
squapi.eye(
	models.LargeLamb.root.Head.eyes.Pupil_left,
	.80, --(.25)leftdistance
	.60, --(1.25)rightdistance
	.5, --(.5)updistance
	.5, --(.5)downdistance
	nil  --(false)switchvalues
)
squapi.eye(
	models.LargeLamb.root.Head.eyes.Pupil_right,
	.60, --(.25)leftdistance
	.80, --(1.25)rightdistance
	.5, --(.5)updistance
	.5, --(.5)downdistance
	nil  --(false)switchvalues
)
squapi.eye(
	models.LargeLamb.root.Head.Crown.pupil_crown,
	.60, --(.25)leftdistance
	.60, --(1.25)rightdistance
	.5, --(.5)updistance
	.5, --(.5)downdistance
	nil  --(false)switchvalues
)
--
squapi.bouncewalk(
	models.LargeLamb.root, --model
	.5  --(1)bounceMultiplier
)
--
squapi.ear(
	models.LargeLamb.root.Head.LeftEar,
	models.LargeLamb.root.Head.RightEar,
	false, --(true))doearflick
	400, --(400)earflickchance
	1, --(1)rangemultiplier
	true, --(false)horizontalEars
	2, --(2)bendstrength
	.025, --(.025)earstiffness
	.1  --(.1)earbounce 
)


--
local mainPage = action_wheel:newPage()

local mainPage = action_wheel:newPage()
action_wheel:setPage(mainPage)

local scroll = 1
local mytextures = {
  textures["LargeLamb.LardLambBMC"],
  textures["LargeLamb.LardLambCasual"],
  textures["LargeLamb.LardLambSemiClothed"],
  textures["LargeLamb.LardLambSemiClothedLA"],
  textures["LargeLamb.LardLambNaked"],
  textures["LargeLamb.LardLambNakedLA"],
  textures["LargeLamb.LardLambHeretic"]
}
function pings.scrolling(value)
  scroll = value
  models:setPrimaryTexture("Custom", mytextures[scroll])
end
local action = mainPage:newAction()
    :title("clothing")
    :item("red_wool")
    :onScroll(function(amount)
      scroll = (((scroll - 1) + amount) % #mytextures) + 1
      pings.scrolling(scroll)
    end)

action_wheel:setPage(mainPage)

function pings.seated(x)
  animations.LargeLamb.seated:setPlaying(x)
end
mainPage:newAction()
    :title("standing")
    :toggleTitle("sitting")
    :item("armor_stand")
    :toggleItem("stone_stairs")
    :onToggle(pings.seated)

action_wheel:setPage(mainPage)

function pings.swelling(x)
  animations.LargeLamb.swell:setPlaying(x)
end
mainPage:newAction()
    :title("normal")
    :toggleTitle("swelling")
    :item("bone")
    :toggleItem("slime_ball")
    :onToggle(pings.swelling)

action_wheel:setPage(mainPage)

function pings.BellyDrop(x)
  animations.LargeLamb.BellyDrop:setPlaying(x)
end
mainPage:newAction()
    :title("belly drop")
    :item("cooked_chicken")
    :hoverColor(1,1,0)
    :onLeftClick(function()
        pings.BellyDrop(math.random())
    end)