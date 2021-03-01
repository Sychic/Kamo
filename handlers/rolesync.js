const axios = require("axios")
const Discord = require("discord.js");
const mongo = require('../mongo.js');


module.exports = async (client) => {
    const guild = await client.guilds.fetch("563932743059111940");
    const guildData = (await axios.get(`https://api.hypixel.net/guild?name=Quacking&key=${process.env.APIKEY}`)).data;
    if (!guildData.success) return;

    let roles = guildData.guild.ranks.map(rankData => guild.roles.cache.find(role => role.name === rankData.name));
    if (roles.size === 0) return;

    mongo.db("users").collection("Users").find().forEach(async ({Discord, uuid}) => {
        const member = guild.member(Discord);
        if (!member) continue;
        let player = guildData.guild.members.find(p => p.uuid === uuid.replace("-", ""));
        if (!player && !member.roles.cache.some(role => role.name === "Floating")) {
            roles.forEach(role => {
                if (member.roles.cache.has(role.id)) member.roles.remove(role);
            });
            continue;
        }

        if (!member.roles.cache.some(role => role.name === player.rank)) {
            roles.forEach(role => {
                if (role.name === player.rank) {
                    member.roles.add(role);
                } else if (member.roles.cache.has(role.id)) member.roles.remove(role);
            });
        }
    });
}