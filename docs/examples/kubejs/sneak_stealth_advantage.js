// Advantage on Stealth checks while crouching. KubeJS server script; requires Checks + KubeJS.
const ChecksApi = Java.loadClass('studio.modroll.checks.api.ChecksApi')

// Server scripts rerun on /reload, but Checks keeps listeners: register once.
if (!global.checksSneakAdvantage) {
  global.checksSneakAdvantage = true
  ChecksApi.onBeforeCheck(event => {
    // An ability's id() is 'dex'; a skill's is its resource location.
    if (String(event.stat().id()) != 'checks:stealth' || !event.entity().isCrouching()) return
    event.grantAdvantage()
    if (event.entity().isPlayer()) event.entity().tell('Sneaking: advantage on Stealth.')
  })
}
