package dev.timetracker.phone.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalBackupTest {
    @Test fun `file name carries the date`() {
        assertEquals("time-tracker-backup-2026-11-22.csv", LocalBackup.fileName(LocalDate.of(2026, 11, 22)))
    }

    @Test fun `prune keeps the newest dates, counting suffixed copies as their date`() {
        val names = listOf(
            "time-tracker-backup-2026-11-22.csv",
            "time-tracker-backup-2026-11-22 (1).csv",
            "time-tracker-backup-2026-11-21.csv",
            "time-tracker-backup-2026-11-19.csv",
            "time-tracker-backup-2026-11-18.csv",
            "time-tracker-backup-2026-11-01 (2).csv",
            "notes.csv",
            "time-tracker-2026-10-26.csv", // a manual export: not ours to delete
        )
        assertEquals(
            setOf("time-tracker-backup-2026-11-18.csv", "time-tracker-backup-2026-11-01 (2).csv"),
            LocalBackup.toPrune(names, keep = 3).toSet(),
        )
    }

    @Test fun `prune keeps everything within the limit`() {
        assertEquals(emptyList<String>(), LocalBackup.toPrune(listOf("time-tracker-backup-2026-11-22.csv"), keep = 3))
    }
}
