// Set emulator env vars before any firebase-admin or firebase-functions modules are loaded.
process.env["GCLOUD_PROJECT"] = "pantry-dev";
process.env["FIRESTORE_EMULATOR_HOST"] = "localhost:8080";
