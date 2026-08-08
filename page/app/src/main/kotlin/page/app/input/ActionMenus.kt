package page.app.input

data class ActionMenu(val title: String, val actions: List<ActionSpec>)

object ActionMenus {

    private val ORDER = listOf(
        ActionGroup.File,
        ActionGroup.Edit,
        ActionGroup.Navigate,
        ActionGroup.Code,
        ActionGroup.View,
        ActionGroup.Page,
    )

    val all: List<ActionMenu> = ORDER.mapNotNull { group ->
        val actions = ActionCatalog.all.filter {
            it.group == group && it.context == ActionContext.Global && it.run != null
        }
        if (actions.isEmpty()) null else ActionMenu(title(group), actions)
    }

    private fun title(group: ActionGroup): String = when (group) {
        ActionGroup.Page -> "PAGE"
        else -> group.name
    }
}
