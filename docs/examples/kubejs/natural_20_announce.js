// Tells a player when any check, save or contest they roll is a natural 20.
const ChecksApi = Java.loadClass('studio.modroll.checks.api.ChecksApi')

if (!global.checksNatural20) {
  global.checksNatural20 = true
  ChecksApi.onAfterCheck(event => {
    if (event.result().isNatural20() && event.entity().isPlayer()) {
      event.entity().tell('Natural 20 on ' + event.stat().id() + '!')
    }
  })
}
