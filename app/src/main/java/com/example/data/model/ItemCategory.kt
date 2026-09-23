package com.example.data.model

enum class ItemCategory(val displayName: String, val defaultQuestions: List<String>) {
    WALLET("Wallet", listOf("What brand or color is the wallet?", "What is inside the wallet (e.g., initial or specific card)?", "Is there cash or coins inside?")),
    PURSE("Purse", listOf("What colour is the inside lining?", "What brand is the purse?", "What items are inside the main compartment?")),
    PHONE("Phone", listOf("What is the phone wallpaper or lock screen image?", "What model or case color is it?", "What sticker or engraving is on the back?")),
    KEYS("Keys", listOf("What is written or shaped on the keychain?", "How many keys are on the ring?", "What color is the lanyard or tag?")),
    ID_CARD("ID Card", listOf("What university or organization issued the card?", "What are the first 2 letters of the name?", "What year or course is listed?")),
    BAG("Bag", listOf("What brand and color is the backpack/bag?", "What distinctive patch or keychain is attached?", "What is inside the front pocket?")),
    ELECTRONICS("Electronics", listOf("What brand and model is the device?", "Is there a case or serial number note?", "What color are the charging accessories?")),
    DOCUMENTS("Documents", listOf("What type of document or certificate is it?", "In what folder or envelope is it kept?", "Whose name or header is on the document?")),
    OTHER("Other", listOf("Describe a unique mark or detail only the owner knows", "What color and material is the item?", "Where and when approximately did you lose it?"));

    companion object {
        fun fromString(name: String): ItemCategory {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) || it.displayName.equals(name, ignoreCase = true) } ?: OTHER
        }
    }
}
