-- API made for treating different models as many outfits that can be changed :>

--[[
    Outfit Import Format
    return {
        name = "",                    -- model's id
        model = models.model,         -- bbmodel
        animations = animations,      -- model animations
        action_wheel = page,          -- the model's own action wheel
        transition = {
            in = animations,          -- transition into the outfit
            out = animations          -- transition out of the outfit
        }, 
        action = {                    -- Outfit's Action data
            name = "",                -- the name displayed
            item = "",                -- the page's item
            toggleitem  = "FFFFFFFF", -- the page's item when selected
            color       = "FFFFFFFF", -- default action color in hex
            toggleColor = "FFFFFFFF"  -- action's color in hex when selected
        }
    }
]]

local configSaveLocation = "loadedOutfit" -- Config id the outfit is saved to when selected

local function formatColor(hex)
    if hex == nil then return end
    assert(type(hex) == "string", "hex needs to be a string dumbass")

    -- Truncate it if it's an 8 byte color
    if #hex == 8 then
        hex = hex:sub(3)
    end

    return vectors.hexToRGB(hex)
end

local outfitAPI = {
    outfits = {},
    actionsMain = nil,
    default = nil,
    selected = nil,
    selected_wheel = nil,

    formatOutfit = function(self, model, _animations, _action_wheel, transition, action)
        return {
            model = model,
            animations = _animations,
            action_wheel = _action_wheel,
            transition = transition,
            action = action
        }
    end,

    formatAction = function(self, name, item, color, toggleItem, toggleColor)
        return {
            name = name,
            item = item,
            toggleItem = toggleItem,
            color = color,
            toggleColor = toggleColor
        }
    end,

    -- Add a new outfit to the internal list, formatting it if necessary 
    new = function(self, outfit, ...)
        -- Make sure it's formatted properly
        if outfit == true then
            outfit = self:formatOutfit(...)
        end
        assert(type(outfit) == "table", "Whoops! Outfit is not a table")

        -- Also make sure it's id wasn't already in the thingamajig
        local name = outfit.name
        assert(type(name) == "string", "The outfit name NEEDS to be a string, sorry")
        assert(not self.outfits[name], ("Outfit %q Already exists apparently"):format(name))

        -- Add it
        -- Having the name in the table was pretty redundant ngl
        self.outfits[name] = outfit
        table.insert(self.outfits, outfit)

        return true
    end,

    setDefault = function(self)
        self.default = self.selected
        return self.default
    end,
}

-- Please don't touch these, i had to patch these pings in so the API would work for multiplayer.
function pings.hideAllOutfits()
    for _, value in pairs(outfitAPI.outfits) do
        value.model:setVisible(false)
    end
end

function pings.setOutfitVis(outfit, bool)
    outfitAPI.outfits[outfit].model:setVisible(bool)
end


-- hides all outfits
function outfitAPI.hideAll(self)
    pings.hideAllOutfits()
    
    -- Untoggle all outfit actions, if they have already been setup
    if not self.actionsMain then
        return false
    end
    
    for name, action in pairs(self.actionsMain) do
        if name ~= self.default then
            action:toggled(false)
        end
    end
    return true
end

function outfitAPI.setOutfit(self, outfitName, override)
    -- Check if the outfit does in fact exist
    assert(type(outfitName) == "string", "Outfit name needs to be a string!")
    assert(self.outfits[outfitName], ("Uhhh... I couldn't find an outfit named %q"):format(outfitName))

    -- If the outfit is already selected then just return
    if outfitName == self.selected then
        return false
    end

    -- actually change it
    local toggled = self:hideAll()
    pings.setOutfitVis(outfitName, true)
    self.selected = outfitName
    self.selected_wheel = self.outfits[outfitName].action_wheel
    if not override and configSaveLocation then
        config:save(configSaveLocation, outfitName)
    end
    
    if toggled then
        self.actionsMain[outfitName]:toggled(true)
    end

    return true
end

-- Make an action wheel page that has all the outfit toggles
function outfitAPI.mkChangePage(self, rightClickFunc)
    -- make page
    local page = action_wheel:newPage()
    local actionList = {}

    for _, outfit in ipairs(self.outfits) do
        local data = outfit.action

        -- Make initial action
        local action = page:newAction()
            :title(data.name)
            :item(data.item)
            :color(formatColor(data.color))
        
        -- Add toggle Item if present
        if data.toggleItem then
            action:toggleItem(data.toggleItem)
        end
        
        -- Add toggle Color if present
        if data.toggleColor then
            action:toggleColor(formatColor(data.toggleColor))
        end

        -- Set the outfit when selected
        if outfit.name == self.default then
            -- If it's the default outfit then make it a normal action
            action:onLeftClick(function()
                self:setOutfit(outfit.name)
            end)
        else
            -- If it's not, then make it a toggle!
            action:onToggle(function(bool)
                if bool then
                    self:setOutfit(outfit.name)
                else
                    self:setOutfit(self.default)
                end
            end)

            -- Remember to also have it on for the selected outfit!
            action:toggled(outfit.name == self.selected)
        end

        -- Doing this so you can return to the main page with right click (do whatever you want tho)
        if rightClickFunc then
            action:onRightClick(rightClickFunc)
        end

        -- And we're done for this action so add it to the list
        actionList[outfit.name] = action
    end
    -- Done!

    self.actionsMain = actionList
    return page
end


return outfitAPI
