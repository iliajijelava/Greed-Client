-- Auto generated script file --

--hide vanilla model
vanilla_model.PLAYER:setVisible(false)

--hide vanilla armor model
vanilla_model.ARMOR:setVisible(false)
--re-enable the helmet item
vanilla_model.HELMET_ITEM:setVisible(true)

--hide vanilla cape model
vanilla_model.CAPE:setVisible(false)

--hide vanilla elytra model
vanilla_model.ELYTRA:setVisible(false)


local mainPage = action_wheel:newPage()
action_wheel:setPage(mainPage)

local mouthState = 1

models.model.Petals:setVisible(false)
models.model.root.RightArm.SmearFrame:setVisible(false)
models.model.root.Head.Mouth.MouthOpen:setVisible(true)
models.model.root.Head.MouthClosed:setVisible(false)

models.model.Knives:setVisible(false)
models.model.Petals:setVisible(false)

models.model.root.Head.MouthGrin:setVisible(false)

models.model.root.Head.BigEye:setVisible(false)
models.model.root.Head.BigEye2:setVisible(false)
models.model.root.Head.SmallEye:setVisible(false)

local greenBowEnabled = false
models.model.root.Head.Hair.Bow:setVisible(true)
models.model.root.Head.Hair.BowGreen:setVisible(false)

local knifeGamePlaying = false
local revealBigEyePlaying = false
local revealThreeEyesPlaying = false
local idleSwapEnabled = false
local pirouettePlaying = false
local knifeSwingTimer = -1

function pings.idleSwap()
    idleSwapEnabled = not idleSwapEnabled

    if idleSwapEnabled then
        animations.model.idleStep:setPlaying(false)
        animations.model.IdleLowCortisol:setPlaying(true)
    else
        animations.model.IdleLowCortisol:setPlaying(false)
        animations.model.idleStep:setPlaying(true)
    end
end

function pings.greenbow()
    greenBowEnabled = not greenBowEnabled

    models.model.root.Head.Hair.Bow:setVisible(not greenBowEnabled)
    models.model.root.Head.Hair.BowGreen:setVisible(greenBowEnabled)
end

function pings.pirouette()
    pirouettePlaying = not pirouettePlaying

    models.model.Petals:setVisible(pirouettePlaying)
    models.model.root.RightArm.SmearFrame:setVisible(
        pirouettePlaying and not knifeGamePlaying
    )

    animations.model.Pirouette:setPlaying(pirouettePlaying)
end

mainPage:newAction()
    :title("Idle Swap")
    :item("minecraft:clock")
    :hoverColor(1, 1, 1)
    :onLeftClick(pings.idleSwap)

mainPage:newAction()
:title("Pirouette")
:item("minecraft:pink_petals")
:hoverColor(0.1, 0, 1)
:onLeftClick(pings.pirouette)

mainPage:newAction()
    :title("Green Bow")
    :item("minecraft:green_wool")
    :hoverColor(0, 1, 0)
    :onLeftClick(pings.greenbow)

function pings.knifegame()
    if revealBigEyePlaying then
        revealBigEyePlaying = false
        animations.model.Reveal1Eye:setPlaying(false)
        models.model.root.Head.BigEye:setVisible(false)
    end

    if revealThreeEyesPlaying then
        revealThreeEyesPlaying = false
        animations.model.Reveal3Eyes:setPlaying(false)
        models.model.root.Head.BigEye2:setVisible(false)
        models.model.root.Head.SmallEye:setVisible(false)
    end

    models.model.root.Head.MouthGrin:setVisible(false)

    knifeGamePlaying = not knifeGamePlaying

    animations.model.KnifeGame:setPlaying(knifeGamePlaying)
    models.model.Knives:setVisible(knifeGamePlaying)

    models.model.root.RightArm.SmearFrame:setVisible(
        pirouettePlaying and not knifeGamePlaying
    )

    if knifeGamePlaying then
        knifeSwingTimer = 1
    else
        knifeSwingTimer = -1
    end
end

mainPage:newAction()
    :title("Knife Game")
    :item("minecraft:iron_sword")
    :hoverColor(0, 1, 1)
    :onLeftClick(pings.knifegame)

function pings.revealBigEye()
    if knifeGamePlaying then
        knifeGamePlaying = false
        animations.model.KnifeGame:setPlaying(false)
        models.model.Knives:setVisible(false)
    end

    if revealThreeEyesPlaying then
        revealThreeEyesPlaying = false
        animations.model.Reveal3Eyes:setPlaying(false)
        models.model.root.Head.BigEye2:setVisible(false)
        models.model.root.Head.SmallEye:setVisible(false)
    end

    revealBigEyePlaying = not revealBigEyePlaying

    animations.model.Reveal1Eye:setPlaying(revealBigEyePlaying)
    models.model.root.Head.BigEye:setVisible(revealBigEyePlaying)
    models.model.root.Head.MouthGrin:setVisible(revealBigEyePlaying)
end

function pings.revealThreeEyes()
    if knifeGamePlaying then
        knifeGamePlaying = false
        animations.model.KnifeGame:setPlaying(false)
        models.model.Knives:setVisible(false)
    end

    if revealBigEyePlaying then
        revealBigEyePlaying = false
        animations.model.Reveal1Eye:setPlaying(false)
        models.model.root.Head.BigEye:setVisible(false)
    end

    revealThreeEyesPlaying = not revealThreeEyesPlaying

    animations.model.Reveal3Eyes:setPlaying(revealThreeEyesPlaying)

    models.model.root.Head.BigEye2:setVisible(revealThreeEyesPlaying)
    models.model.root.Head.SmallEye:setVisible(revealThreeEyesPlaying)
    models.model.root.Head.MouthGrin:setVisible(revealThreeEyesPlaying)
end

mainPage:newAction()
    :title("Reveal 3 Eyes")
    :item("minecraft:spider_eye")
    :hoverColor(1, 1, 0)
    :onLeftClick(pings.revealThreeEyes)

mainPage:newAction()
    :title("Reveal Big Eye")
    :item("minecraft:ender_eye")
    :hoverColor(0.6, 0, 1)
    :onLeftClick(pings.revealBigEye)

function pings.mouth()
    mouthState = mouthState + 1
    if mouthState > 3 then
        mouthState = 1
    end

    if mouthState == 1 then
        models.model.root.Head.Mouth:setVisible(true)
        models.model.root.Head.Mouth.MouthOpen:setVisible(false)
        models.model.root.Head.MouthClosed:setVisible(false)

    elseif mouthState == 2 then
    models.model.root.Head.Mouth:setVisible(false)
    models.model.root.Head.Mouth.MouthOpen:setVisible(false)
    models.model.root.Head.MouthClosed:setVisible(true)

    else
        models.model.root.Head.Mouth:setVisible(true)
        models.model.root.Head.Mouth.MouthOpen:setVisible(true)
        models.model.root.Head.MouthClosed:setVisible(false)
    end
end

mainPage:newAction()
    :title("Mouth")
    :item("minecraft:apple")
    :hoverColor(1, 0.5, 0)
    :onLeftClick(pings.mouth)

local wasRunning = false

models.model.root.Head.Hair.HairOutline:setPrimaryRenderType("CUTOUT_CULL")

animations.model.KnifeSpin:setPlaying(true)

function events.tick()
    local crouching = player:getPose() == "CROUCHING"
        animations.model.Crouch:setPlaying(crouching)
    local sprinting = player:isSprinting()
    local sleeping = player:getPose() == "SLEEPING"
    local swimming = player:getPose() == "SWIMMING"
    local flying = player:getPose() == "FALL_FLYING"
    local walking = player:getVelocity().xz:length() > 0.01
    local crouchWalking = walking and crouching

    if knifeSwingTimer >= 0 then
    knifeSwingTimer = knifeSwingTimer - 1

    if knifeSwingTimer == 0 then
        sounds:playSound("snd_smallswing", player:getPos())
        knifeSwingTimer = -1
    end
end

    if pirouettePlaying then
        animations.model.idleStep:setPlaying(false)
        animations.model.IdleLowCortisol:setPlaying(false)
        animations.model.walk:setPlaying(false)
        animations.model.CrouchWalk:setPlaying(false)
        animations.model.run:setPlaying(false)
        wasRunning = false
        return
    end

    if idleSwapEnabled then
        animations.model.idleStep:setPlaying(false)
        animations.model.IdleLowCortisol:setPlaying(
            not walking and
            not crouching
        )
    else
        animations.model.IdleLowCortisol:setPlaying(false)
        animations.model.idleStep:setPlaying(
            not walking and
            not crouching
        )
    end

    animations.model.walk:setPlaying(
    walking and
    not sprinting and
    not crouching
)

animations.model.CrouchWalk:setPlaying(
    crouchWalking
)

    if sprinting and not crouching then
        if not wasRunning then
            animations.model.run:setPlaying(true)
            wasRunning = true
        end
    else
        if wasRunning then
            animations.model.run:setPlaying(false)
            wasRunning = false
        end
    end
end