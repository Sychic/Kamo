const mongodb = require('mongodb');

const mongo = new mongodb.MongoClient(process.env.DB, {
	useUnifiedTopology: true,
	useNewUrlParser: true,
	});
try {
	mongo.connect();
} catch (error) {
	console.error(error);
}
module.exports = mongo;