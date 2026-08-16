vanilla_model.PLAYER:setVisible(false)
vanilla_model.ARMOR:setVisible(false)
vanilla_model.HELMET_ITEM:setVisible(true)
vanilla_model.CAPE:setVisible(false)

local configDef = require "libraries.defaults".default
local outfitAPI = require "libraries.outfitAPI"

-- Import all outfits from their files
local default = require "outfits.default"
local cowgirl = require "outfits.cowgirl"
local fighter = require "outfits.fighter"
local desert = require "outfits.desert"
local soldier = require "outfits.soldier"
local swimsuit = require "outfits.swimsuit"
local greek = require "outfits.greek"
local summer = require "outfits.summer"
local winter = require "outfits.winter"
local lawyer = require "outfits.lawyer"
local kibbleStar = require "outfits.kibbleStar"

-- load outfits into API
outfitAPI:new(default)
outfitAPI:new(kibbleStar)
outfitAPI:new(cowgirl)
outfitAPI:new(fighter)
outfitAPI:new(desert)
outfitAPI:new(soldier)
outfitAPI:new(swimsuit)
outfitAPI:new(greek)
outfitAPI:new(summer)
outfitAPI:new(winter)
outfitAPI:new(lawyer)

-- Set the default outfit
outfitAPI:setOutfit("default", true)
outfitAPI:setDefault()


-- -- Config, Pings and misc

configDef("loadedOutfit", "default")
configDef("kaboodle", true)

outfitAPI:setOutfit(config:load("loadedOutfit"))

local function formatPage(page, func)
    for _, value in pairs(page:getActions()) do
        value:onRightClick(func)
    end
end

function pings.kaboodle(bool)
    models.kaboodle:setVisible(bool)
    config:save("kaboodle", bool)
end


pings.kaboodle(config:load("kaboodle"))

-- -- Action wheel

local mainPage = action_wheel:newPage()
action_wheel:setPage(mainPage)

local function returnToMain() action_wheel:setPage(mainPage) end
local outfitPage = outfitAPI:mkChangePage(returnToMain)

mainPage:newAction()
    :title("Outfits")
    :item("netherite_chestplate")
    :onLeftClick(function() action_wheel:setPage(outfitPage) end)

mainPage:newAction()
    :title("Outfit Actions")
    :item("gold_block")
    :onLeftClick(function()
        formatPage(outfitAPI.selected_wheel, returnToMain)

        action_wheel:setPage(outfitAPI.selected_wheel)
    end)

mainPage:newAction()
    :title("Kaboodle (JaySchlatt)")
    :item("white_wool")
    :toggleItem("iron_block")
    :toggleColor(vectors.hexToRGB("D0C8C8"))
    :onToggle(pings.kaboodle)
    :toggled(config:load("kaboodle"))


-- Make sure kaboodle backpack doesn't show on kibbleStar outfit
local wasKibble = false
function events.TICK()
    if outfitAPI.selected == "kibbleStar" then
        if wasKibble then return end

        models.kaboodle.Body:setVisible(false)
        wasKibble = true
        return
    end

    if wasKibble then
        models.kaboodle.Body:setVisible(true)
        wasKibble = false
    end
end

-- -- SquishAPI and Gaze stuff

local squapi = require "libraries.SquAPI"
local gazeapi = require "libraries.Gaze"

-- save the kibbleStar gaze object for later
local gazes = {}

-- Do everything that needs to be done for all outfits
for _, outfit in ipairs(outfitAPI.outfits) do
    local model, animation = outfit.model, outfit.animations

    -- DEPRECATED (Implemented gaze instead :> )
    -- Eyes
    -- squapi.eye:new(model.root.Head.Eyes.EyeL, .4, .8, .3, .3)
    -- squapi.eye:new(model.root.Head.Eyes.EyeR, .8, .4, .3, .3)
    --
    -- -- Blinking animation
    -- squapi.randimation:new(animation.blink, 200, true)
    --
    -- -- Smooth Head movement
    -- squapi.smoothHead:new(
    --     model.root.Head,
    --     nil,
    --     nil,
    --     1.75
    -- )

    local tailSegs = {
        model.root.Body.Tail,
        model.root.Body.Tail.Tail1,
        model.root.Body.Tail.Tail1.Tail2,
        model.root.Body.Tail.Tail1.Tail2.Tail3,
        model.root.Body.Tail.Tail1.Tail2.Tail3.Tail4,
    }

    -- Tail Physics
    squapi.tail:new(
        tailSegs,
        nil,
        nil,
        .6,
        .5
    )

    -- KibbleStar is different so we're skipping to do it later
    -- setup boob physics (kibbleStar won't have it cus it's armor)
    if outfit.name ~= "kibbleStar" then
        squapi.bewb:new(
            model.root.Body.Boobs,
            nil,
            nil,
            nil,
            true,
            2,
            nil,
            nil,
            8
        )
    end

    -- Do Ear physics for all outfits that have them
    pcall(function()
        squapi.ear:new(
            model.root.Head.Ears.EarLeft,
            model.root.Head.Ears.EarRight,
            .2,
            false,
            .75,
            true,
            200,
            .1,
            .6
        )
    end)

    -- -- Setup Gaze
    local gaze = gazeapi:newGaze(model.root.Head, model.root.Head.Eyes)
    gaze.config.turnStrength = 22.5
    gaze.config.turnDampen = .75

    -- Eyes
    gaze:newEye(model.root.Head.Eyes.EyeL, .4, .7, .3, .3)
    gaze:newEye(model.root.Head.Eyes.EyeR, .7, .4, .3, .3)

    -- Blink animation
    gaze:newBlink(animation.blink)

    -- Save gazes
    gazes[outfit.name] = gaze
end

-- Do the thing for the kibbleStar goggles + Kaboodle Eyes

-- DEPRECATED (Implemented gaze instead :> )
-- squapi.eye:new(models.kibbleStar.root.Head.Helmet.Visor.GoggleEyes.EyeL2, .4, .8, .3, .3)
-- squapi.eye:new(models.kibbleStar.root.Head.Helmet.Visor.GoggleEyes.EyeR2, .8, .4, .3, .3)
-- squapi.randimation:new(animations.kibbleStar.blink, 200, true)

gazes.kibbleStar:newEye(models.kibbleStar.root.Head.Helmet.Visor.GoggleEyes.EyeL2, .4, .8, .3, .3)
gazes.kibbleStar:newEye(models.kibbleStar.root.Head.Helmet.Visor.GoggleEyes.EyeR2, .8, .4, .3, .3)

gazes.kibbleStar:newBlink(animations.kibbleStar.blink)

-- Shoulder kaboodl

-- DEPRECATED (Implemented gaze instead :> )
-- squapi.eye:new(models.kibbleStar.root.LeftArm.Kaboodle.KaboodleEye, .25, .25, .25, .25)
-- squapi.randimation:new(animations.kibbleStar.kaboodleBlink, 200, true)

local gaze = gazeapi:newGaze(models.kibbleStar.root.LeftArm.Kaboodle, models.kibbleStar.root.LeftArm.Kaboodle.KaboodleEye)
gaze:newEye(models.kibbleStar.root.LeftArm.Kaboodle.KaboodleEye, .25, .25, .25, .25)
gaze:newBlink(animations.kibbleStar.kaboodleBlink)
gaze.config.turnStrength = 0

-- Given kaboodle a gaze

gaze = gazeapi:newGaze(models.kaboodle.Body.Kaboodle.main, models.kaboodle.Body.Kaboodle.main.Eye)
gaze:newEye(models.kaboodle.Body.Kaboodle.main.Eye, .5, .5, .5, .5)
gaze:newBlink(animations.kaboodle.blink)
gaze.config.turnStrength = -11.25

-- squapi.eye:new(models.kaboodle.Body.Kaboodle.Eye, .5, .5, .5, .5)
-- squapi.randimation:new(animations.kaboodle.blink, 200, true)

-- -- Swing Phyisics for fighter outfit
local swing = require "libraries.swinging_physics"

-- Left side
local root = models.fighter.root.Head.Bandana.StrandsLeft
swing.swingOnHead(
    root,
    180, {-135, 0, -360, 360, -120, 61},
    nil, nil, false
)
swing.swingOnHead(
    root.StrandsLeft2,
    180, {-65, 30, -360, 360, -20, 20},
    root, 1, false
)
swing.swingOnHead(
    root.StrandsLeft2.StrandsLeft3,
    180, {-65, 30, -360, 360, -20, 20},
    root, 1.5, false
)
swing.swingOnHead(
    root.StrandsLeft2.StrandsLeft3.StrandsLeft4,
    180, {-65, 30, -360, 360, -20, 20},
    root, 2, false
)

-- Right Side
root = models.fighter.root.Head.Bandana.StrandsRight
swing.swingOnHead(
    root,
    180, {-135, 0, -360, 360, -120, 60},
    nil, nil, false
)
swing.swingOnHead(
    root.StrandsRight2,
    180, {-65, 30, -360, 360, -20, 20},
    root, 1, false
)
swing.swingOnHead(
    root.StrandsRight2.StrandsRight3,
    180, {-65, 30, -360, 360, -20, 20},
    root, 1.5, false
)
swing.swingOnHead(
    root.StrandsRight2.StrandsRight3.StrandsRight4,
    180, {-65, 30, -360, 360, -20, 20},
    root, 2, false
)

-- Do the other cloth thingy
root = models.fighter.root.Body.cloth
swing.swingOnBody(
    root,
    0, {0, 135, -360, 360, -5, 5},
    nil, nil, false
)
swing.swingOnBody(
    root.cloth1,
    0, {0, 135, -360, 360, -5, 5},
    root, 1, false
)
swing.swingOnBody(
    root.cloth1.cloth2,
    0, {-35, 135, -360, 360, -5, 5},
    root, 1.5, false
)
swing.swingOnBody(
    root.cloth1.cloth2.cloth3,
    0, {-35, 135, -360, 360, -5, 5},
    root, 2, false
)

-- Other stuff

-- TODO: Desert Skirt Physics
-- TODO: Add Physics to the winter dress


