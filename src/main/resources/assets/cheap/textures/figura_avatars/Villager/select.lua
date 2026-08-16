--- @class Select.Data
--- @field title string?
--- @field choices {[1]:any,[2]:any}[]
--- @field selected number
--- @field color string
--- @field selected_color string
--- @field separator_color string
--- @field left_click function?
--- @field right_click function?

local original_action_index = figuraMetatables.Action.__index
--- @class Action
local Action = {}
---@type {[Action]: Select.Data}
local _selectData = {}

local function update_select(action)
    local dt = _selectData[action]
    local title = {
        {text=""},"" -- appease linter (blegh)
    }

    if dt.title then
        title[#title+1] = dt.title
        title[#title+1] = "\n"
        title[#title+1] = {text="-",color = dt.separator_color}
        title[#title+1] = "\n"
    end

    for index, value in ipairs(dt.choices) do
        local v = {}
        if index == dt.selected then
            v[#v+1] = {text = "[> ", color = dt.selected_color}
        else
            v[#v+1] = {text = "[ ", color = dt.color}
        end
        v[#v+1] = value[1]
        title[#title+1] = v
        title[#title+1] = "\n"
    end
    if #title > 2 then
        title[#title] = nil
    end

    if dt.title then
        local width = client.getTextWidth(toJson(title))
        title[5].text = string.rep("-", math.round(width/6))
    end

    original_action_index(action, "setTitle")(action, toJson(title))
end

function figuraMetatables.Action:__index(key)
    return Action[key] or original_action_index(self,key)
end

---Sets the title that appears when this action is hovered over.
---
---If `title` is `nil`, it will default to `""`.
---@generic self
---@param self self
---@param title? string | table
---@return self
function Action:setTitle(title)
    if not host:isHost() then return self end
    if _selectData[self] then
        _selectData[self].title = title
        update_select(self)
        return self
    else
        return original_action_index(self, "setTitle")(self, title)
    end
end

Action.title = Action.setTitle

---Directly sets the internal choices table of the Select.
---@param choices ({[1]: string|table, [2]: any?})[]
---@return Action
function Action:setChoices(choices)
    local dt = _selectData[self] 
    assert(dt, "Cannot call :setChoices() on non-select Action!")
    local c = {}
    for index, value in ipairs(choices) do
        local v = {value[1]}
        if value[2] == nil then v[2] = value[1]
        else v[2] = value[2] end
        c[index] = v
    end
    dt.choices = c
    update_select(self)
    return self
end

---Adds a choice to this Select. 
---
---If value is nil, name will be used instead.
---@param name string | table
---@param value any?
---@return Action
function Action:addChoice(name, value)
    local dt = _selectData[self] 
    assert(dt, "Cannot call :addChoice() on non-select Action!")
    value = value or name
    dt.choices[#dt.choices+1] = {name, value}
    update_select(self)
    return self
end

---Sets the focused color of options in this Select
---@param self Action
---@param col string
---@return self
function Action:setFocusedColor(col)
    local dt = _selectData[self] 
    assert(dt, "Cannot call :setFocusedColor() on non-select Action!")
    dt.selected_color = col
    update_select(self)
    return self
end

---Sets the unfocused color of options in this Select
---@param self Action
---@param col string
---@return self
function Action:setUnfocusedColor(col)
    local dt = _selectData[self] 
    assert(dt, "Cannot call :setUnfocusedColor() on non-select Action!")
    dt.color = col
    update_select(self)
    return self
end

---Sets the color of the separator of this Select.
---@param self Action
---@param col string
---@return self
function Action:setSeparatorColor(col)
    local dt = _selectData[self] 
    assert(dt, "Cannot call :setSeparatorColor() on non-select Action!")
    dt.separator_color = col
    update_select(self)
    return self
end

---Sets the function that executed when this action is left-clicked.
---
---If this action is a Select, the selected value is passed
---as the second argument.
---@param func Action.clickFunc | fun(self: Action, value: any)
---@return Action
function Action:setOnLeftClick(func)
    if not host:isHost() then return self end
    if _selectData[self] then
        _selectData[self].left_click = func
        return self
    else
        return original_action_index(self, "setOnLeftClick")(self, func)
    end
end

---Sets the function that executed when this action is lrighteft-clicked.
---If this action is a Select, the selected value is passed
---as the second argument.
---@param func Action.clickFunc | fun(self: Action, value: any)
---@return Action
function Action:setOnRightClick(func)
    if not host:isHost() then return self end
    if _selectData[self] then
        _selectData[self].right_click = func
        return self
    else
        return original_action_index(self, "setOnRightClick")(self, func)
    end
end



local original_page_index = figuraMetatables.Page.__index
--- @class Page
local Page = {}
function figuraMetatables.Page:__index(key)
    return Page[key] or original_page_index(self, key)
end

---Creates a new `Action` with Select capabilities inside of this page.
---
---If `index` is `nil`, the action will be placed in the first available slot.
---@param index? integer
---@return Action
function Page:newSelect(index)
    local action = self:newAction(index)
    _selectData[action] = {
        title = nil,
        choices = {},
        selected = 1,
        color = "#777777",
        selected_color = "#FFFFFF",
        separator_color = "#444444"
    }
    if not host:isHost() then return action end
    local dt = _selectData[action]
    action:setOnScroll(function (dir, self)
        dir = math.sign(dir) * -1 -- normalize to -1, 0, 1
        dt.selected = ((dt.selected + dir - 1) % #dt.choices) + 1
        sounds:playSound("minecraft:ui.button.click")
        update_select(self)
    end)

    original_action_index(action, "setOnLeftClick")(action, function (self)
        local value = dt.choices[dt.selected]
        if value then
            sounds:playSound("minecraft:ui.button.click")
            if dt.left_click then dt.left_click(self, value[2]) end
        end
    end)

    original_action_index(action, "setOnRightClick")(action, function (self)
        local value = dt.choices[dt.selected]
        if value then
            sounds:playSound("minecraft:ui.button.click")
            if dt.right_click then dt.right_click(self, value[2]) end
        end
    end)

    return action
end