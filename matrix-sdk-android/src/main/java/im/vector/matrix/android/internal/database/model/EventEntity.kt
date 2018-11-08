package im.vector.matrix.android.internal.database.model

import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey

internal open class EventEntity(@PrimaryKey var eventId: String = "",
                       var type: String = "",
                       var content: String = "",
                       var prevContent: String? = null,
                       var stateKey: String? = null,
                       var originServerTs: Long? = null,
                       var sender: String? = null,
                       var age: Long? = 0,
                       var redacts: String? = null
) : RealmObject() {

    companion object

    @LinkingObjects("events")
    val chunk: RealmResults<ChunkEntity>? = null

}