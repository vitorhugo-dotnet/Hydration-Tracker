package com.example

import com.example.data.WaterLog
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun gamification_streakAndBadgeProgress_isCorrect() {
      val goal = 2000
      val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
      
      // Logs on consecutive days reaching the goal
      val cal = Calendar.getInstance()
      val todayTime = cal.timeInMillis
      
      cal.add(Calendar.DAY_OF_YEAR, -1)
      val yesterdayTime = cal.timeInMillis
      
      cal.add(Calendar.DAY_OF_YEAR, -1)
      val twoDaysAgoTime = cal.timeInMillis

      val logs = listOf(
          WaterLog(id = 1, amountMl = 1100, timestamp = todayTime),
          WaterLog(id = 2, amountMl = 1000, timestamp = todayTime), // Sum 2100 mL -> Goal achieved for today!
          
          WaterLog(id = 3, amountMl = 2200, timestamp = yesterdayTime), // Sum 2200 mL -> Goal achieved for yesterday!
          
          WaterLog(id = 4, amountMl = 800, timestamp = twoDaysAgoTime) // Sum 800 mL -> Goal missed for 2 days ago!
      )

      // Let's analyze the expected group and results:
      val logsByDay = logs.groupBy { sdf.format(Date(it.timestamp)) }
      val metDays = logsByDay.filter { (_, dayLogs) ->
          dayLogs.sumOf { it.amountMl } >= goal
      }.keys.toSet()

      // Asserts that today and yesterday are classified under goal-achieved days
      assertTrue(metDays.contains(sdf.format(Date(todayTime))))
      assertTrue(metDays.contains(sdf.format(Date(yesterdayTime))))
      assertFalse(metDays.contains(sdf.format(Date(twoDaysAgoTime))))

      // Asserts total volume
      val totalVolume = logs.sumOf { it.amountMl }
      assertEquals(5100, totalVolume)
  }
}
