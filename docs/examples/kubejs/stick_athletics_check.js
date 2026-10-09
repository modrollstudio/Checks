// Right-clicking with a stick rolls Athletics against DC 12 through Checks (and so through Critfall).
// The KubeJS hook name varies by version; the ChecksApi calls are the stable part.
const ChecksApi = Java.loadClass('studio.modroll.checks.api.ChecksApi')
const ResourceLocation = Java.loadClass('net.minecraft.resources.ResourceLocation')

ItemEvents.rightClicked('minecraft:stick', event => {
  const player = event.player
  const athletics = ChecksApi.skill(ResourceLocation.parse('checks:athletics')).orElseThrow()
  const roll = ChecksApi.check(player, athletics, 12)
  if (roll.canceled()) return
  const result = roll.result()
  player.tell('Athletics vs DC 12: d20 ' + result.natural() + ' = ' + result.saveTotal()
    + (result.saved() ? ', success' : ', failure'))
})
