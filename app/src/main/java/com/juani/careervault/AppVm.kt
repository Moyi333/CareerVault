package com.juani.careervault

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class AppVm(app: Application) : AndroidViewModel(app) {
    val dao = CvDb.get(app).dao()

    fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    fun addCareer(dt: String, onDone: (Long) -> Unit) {
        viewModelScope.launch { onDone(dao.insertCareer(Career(dt = dt))) }
    }

    fun saveEntry(e: Entry) = launch { if (e.id == 0L) dao.insertEntry(e) else dao.updateEntry(e) }
    fun deleteEntry(e: Entry) = launch { dao.deleteEntry(e) }

    /** Guarda club/selección; si no tiene escudo lo busca solo en internet. */
    fun saveStint(s: Stint) {
        viewModelScope.launch {
            val id = if (s.id == 0L) dao.insertStint(s) else { dao.updateStint(s); s.id }
            if (s.crest.isBlank()) {
                val path = Crests.search(getApplication(), s.name, s.kind == "NT")
                if (path != null) dao.stintOnce(id)?.let { dao.updateStint(it.copy(crest = path)) }
            }
        }
    }

    fun saveRival(r: Rival, national: Boolean) {
        viewModelScope.launch {
            val id = if (r.id == 0L) dao.insertRival(r) else { dao.updateRival(r); r.id }
            if (r.crest.isBlank()) {
                val path = Crests.search(getApplication(), r.name, national)
                if (path != null) dao.rivalOnce(id)?.let { dao.updateRival(it.copy(crest = path)) }
            }
        }
    }

    fun deleteRival(r: Rival) = launch { dao.deleteByRef("DERBY", r.id); dao.deleteRival(r) }
    fun deleteStatPlayer(p: StatPlayer) = launch { dao.deleteByRef("STAT", p.id); dao.deleteStatPlayer(p) }

    fun createSquad(name: String, formation: String) = launch {
        val id = dao.insertSquad(Squad(name = name, formation = formation))
        layout(formation).forEach { (code, x, y) ->
            dao.insertSquadPlayer(SquadPlayer(squadId = id, pos = code, starter = true, x = x, y = y))
        }
    }

    fun applyFormation(sq: Squad, players: List<SquadPlayer>, f: String) = launch {
        dao.updateSquad(sq.copy(formation = f))
        val starters = players.filter { it.starter }.sortedBy { it.id }
        layout(f).forEachIndexed { i, (code, x, y) ->
            val ex = starters.getOrNull(i)
            if (ex != null) dao.updateSquadPlayer(ex.copy(x = x, y = y, pos = if (ex.name.isBlank()) code else ex.pos))
            else dao.insertSquadPlayer(SquadPlayer(squadId = sq.id, pos = code, starter = true, x = x, y = y))
        }
    }

    fun saveSquadPlayer(p: SquadPlayer) = launch { if (p.id == 0L) dao.insertSquadPlayer(p) else dao.updateSquadPlayer(p) }
}
