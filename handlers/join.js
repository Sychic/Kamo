const Discord = require('discord.js');

module.exports = function (user) {
    let ducksRole = user.guild.roles.cache.find(role => role.name == "Ducks");
    let unverified = user.guild.roles.cache.find(role => role.name == "Unverified");
    user.roles.add(ducksRole, "Auto duck role")
        .then(() => {
            user.roles.add(unverified, "Auto Unverified");
        });
}