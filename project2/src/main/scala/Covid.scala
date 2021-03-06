import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.sql.SQLContext
import java.sql.DriverManager
import java.sql.Connection
import java.util.Scanner
import scala.collection.mutable.ArrayBuffer
import org.apache.spark.sql.functions
import org.apache.spark.sql.functions.{min, max}
import org.apache.spark.sql.Row
import org.apache.spark.sql.execution.aggregate.HashAggregateExec
import org.apache.spark.sql.execution.joins.BroadcastHashJoinExec
import org.apache.spark.sql.expressions.scalalang.typed
import org.apache.spark.sql.functions.{avg, broadcast, col, max}
import org.apache.spark.sql.types
import org.apache.hadoop.fs.HardLink
import org.apache.spark.sql.functions.{to_date, to_timestamp}

// second changes made here

object Covid {
  private var scanner = new Scanner(System.in)
  val conf = new SparkConf().setMaster("local").setAppName("Covid")
  private val sc = new SparkContext(conf)
  private val hiveCtx = new HiveContext(sc)
  def main(args: Array[String]): Unit = {
    // This block of code is all necessary for spark/hive/hadoop
    
    System.setSecurityManager(null)
    System.setProperty("hadoop.home.dir", "C:\\hadoop\\") // change if winutils.exe is in a different bin folder
    // val conf = new SparkConf()
    //     .setMaster("local") 
    //     .setAppName("Covid")    // Change to whatever app name you want
    //val sc = new SparkContext(conf)
    sc.setLogLevel("ERROR")
    //val hiveCtx = new HiveContext(sc)
    import hiveCtx.implicits._
    val spark = SparkSession
    .builder
    .appName("Covid")
    .config("spark.master", "local")
    .config("spark.eventLog.enabled", "false")
    .getOrCreate()

    //insertCovidData(hiveCtx)
    covidDataMainMenu()
    spark.stop()


  }

  def covidDataMainMenu(): Unit = {        
    //insertCovidData(hiveCtx)        
    var getInCovidData = true        
    while (getInCovidData) {           
      println("")            
      println("Please choose one of the options below.")            
      println("[1] Top 10 deaths in the US")            
      println("[2] Top 10 confirmed cases in the US")            
      println("[3] Bottom 10 confirmed cases in the US")            
      println("[4] Top 10 deaths by continent")           
      println("[5] Top 10 confirmed cases by continent")           
      println("[6] Top 10 vaccinated continents")  
      println("[7] Top 10 confirmed cases in (5/2/2021) by location")
      println("[8] Bottom 10 confirmed cases in (5/2/2021) by location")
      println("[9] Bottom 10 confirmed cases by location")
      println("[10] Top 10 confirmed cases by location")
      println("[11] The trend in deaths in the US by date")          
      println("To exit from this page just touch zero")            
      println("++++++++++++++++++++++++++++++++++++++++++++++++++++++")            
      var userInput = scanner.next().toString()            
      if (userInput == "1") {                
        Top10DeathsUS(hiveCtx)            
      }            
      else if (userInput == "2") {                
        Top10ConfirmedCasesUS(hiveCtx)           
      }            
      else if (userInput == "3") {                
        Bottom10ConfirmedCasesUS(hiveCtx)           
      }            
      else if (userInput == "4") {                
       top10DeathsByContinent(hiveCtx)          
      }           
      else if (userInput == "5") {                
       TotalCasesByContinent(hiveCtx)           
      }            
      else if (userInput == "6") {                
        TotalVaccination(hiveCtx)           
      }
      else if (userInput == "7") {                
        top10CasesByDate(hiveCtx)           
      }
      else if (userInput == "8") {                
        btm10CasesByDate(hiveCtx)          
      }
      else if (userInput == "9") {                
        Bottom10TotalCasesLocation(hiveCtx)          
      }
      else if (userInput == "10") {                
        Top10CasesByLocation(hiveCtx)          
      }
      else if (userInput == "11") {
        TrendInUSByDeaths(hiveCtx)
      }            
      else if (userInput == "0") {                
        getInCovidData = false                         
      }            
      else {               
        println("Invalid input please try again.")           
      }
    }
  }
        
    
    def insertCovidData(hiveCtx:HiveContext): Unit = {

        val output = hiveCtx.read
            .format("csv")
            .option("inferSchema", "true")
            .option("header", "true")
            .load("input/covid-data.csv")
        //output.limit(15).show() // Prints out the first 15 lines of the dataframe

        // output.registerTempTable("data2") // This will create a temporary table from your dataframe reader that can be used for queries. 

        output.createOrReplaceTempView("temp_data")
        hiveCtx.sql("DROP TABLE IF EXISTS covid1")
        hiveCtx.sql("CREATE TABLE IF NOT EXISTS covid1 (iso_code STRING,continent STRING,location STRING,date STRING,total_cases DOUBLE,new_cases DOUBLE,total_deaths DOUBLE,new_deaths DOUBLE,new_tests DOUBLE,total_tests DOUBLE,total_vaccinations DOUBLE,people_vaccinated DOUBLE,people_fully_vaccinated DOUBLE,population INT,population_density FLOAT,median_age FLOAT,aged_65_older FLOAT,aged_70_older FLOAT,gdp_per_capita FLOAT,hospital_beds_per_thousand FLOAT,life_expectancy FLOAT)")
        hiveCtx.sql("INSERT INTO covid1 SELECT * FROM temp_data")
        val summary = hiveCtx.sql("SELECT continent, location, total_cases FROM covid1 LIMIT 10")
        summary.show()

    }
    

    //Maiya
    def Top10DeathsUS(hiveCtx:HiveContext): Unit = {
        println("=== Top 10 death in the United States ===")
      
        val result = hiveCtx.sql("SELECT location, date, total_deaths AS Deaths FROM covid1 WHERE location = 'United States' ORDER BY total_deaths DESC LIMIT 10")
        
        result.show()
        result.repartition(1).write.format("com.databricks.spark.csv").option("header", "true").mode("overwrite").save("results/Top10DeathsUS")
    }
    //Maiya
    def Bottom10TotalCasesLocation(hiveCtx:HiveContext): Unit = {
        println("=== Bottom 10 total cases by location ===")

        val result = hiveCtx.sql("SELECT location, date, (total_cases) AS MinimumTotalCases FROM covid1 WHERE total_cases >= 0.0 GROUP BY location, date, MinimumTotalCases ORDER BY MinimumTotalCases ASC LIMIT 10")
        result.show()
        result.repartition(1).write.format("com.databricks.spark.csv").option("header", "true").mode("overwrite").save("results/Bottom10TotalCasesLocation")
    }
    //Maiya
    def Top10CasesByLocation(hiveCtx:HiveContext): Unit = {
        println("=== Top 10 total cases by location ===")
        val result = hiveCtx.sql("SELECT location, date, MAX(total_cases) AS MaximumTotalCases FROM covid1 GROUP BY location, date ORDER BY MaximumTotalCases DESC LIMIT 10")
        result.show()
        result.repartition(1).write.format("com.databricks.spark.csv").option("header", "true").mode("overwrite").save("results/Top10CasesByLocation")
    }
    //This is the trend
    def TrendInUSByDeaths(hiveCtx:HiveContext): Unit = {    
        println("=== Trend of deaths in the US by date ===")
        val result = hiveCtx.sql("SELECT iso_code, date, (new_cases) NewCases, (people_vaccinated) Vaccinated, (new_deaths) Deaths FROM covid1 WHERE iso_code='USA' AND date IN ('12/14/2020', '1/15/2021','2/15/2021','3/15/2021','4/15/2021','5/15/2021','6/15/2021','7/15/2021','8/15/2021','9/15/2021','10/15/2021','11/15/2021','12/15/2021') GROUP BY iso_code, date, NewCases, Vaccinated, Deaths ORDER BY date")
        
        result.show()
        result.repartition(1).write.format("com.databricks.spark.csv").option("header", "true").mode("overwrite").save("results/TrendingInDeaths")


   
    }

    // changed by wakgari
    def top10DeathsByContinent(hiveCtx:HiveContext): Unit = {
        println("=== Top 10 deaths by continent ===")
        val result = hiveCtx.sql("SELECT continent, MAX(total_deaths) Total_Deaths FROM covid1 WHERE continent IS NOT NULL GROUP BY continent ORDER BY Total_Deaths DESC LIMIT 10")
        result.show()
        result.repartition(1).write.format("com.databricks.spark.csv").option("header", "true").mode("overwrite").save("results/top10DeathsByContinent")
    }

    // Wakgari
    def TotalCasesByContinent(hiveCtx:HiveContext): Unit = {
        println("== Top 10 confirmed cases by continent ==")
        val result = hiveCtx.sql("SELECT DISTINCT continent, MAX(total_cases) Max_cases from covid1 WHERE continent IS NOT NULL GROUP BY continent ORDER BY Max_cases DESC LIMIT 10")
        result.show()
        result.repartition(1).write.format("com.databricks.spark.csv").option("header", "true").mode("overwrite").save("results/TotalCasesByContinent")
    }

    // Wakgari
    def TotalVaccination(hiveCtx:HiveContext): Unit = {
        println("=== Top 10 total vaccinations by continent ===")
        val result = hiveCtx.sql("SELECT continent, MAX(total_vaccinations) AS Max_Total_vaccination from covid1 WHERE continent IS NOT NULL GROUP BY continent ORDER BY Max_Total_vaccination DESC LIMIT 10")
        result.show()
        result.repartition(1).write.format("com.databricks.spark.csv").option("header", "true").mode("overwrite").save("results/TotalVaccination")
       
    }
    //Sharyar
    def Top10ConfirmedCasesUS(hiveCtx:HiveContext): Unit = {
        println("=== Top 10 total confirmed cases in US ===")
        val result = hiveCtx.sql("SELECT location, date, MAX(total_cases) AS Max_Total_Cases from covid1 WHERE location = 'United States' GROUP BY location, date ORDER BY Max_Total_Cases DESC LIMIT 10")
        result.show()
        //result.write.mode("overwrite").csv("results/Top10ConfirmedCasesUS")
       
    }
    //Sharyar
    def Bottom10ConfirmedCasesUS(hiveCtx:HiveContext): Unit = {
        println("=== Bottom 10 confirmed cases in US ===")
        val result = hiveCtx.sql("SELECT location, date, Min(total_cases) AS Min_Total_Cases from covid1 WHERE location = 'United States' GROUP BY location, date ORDER BY Min_Total_Cases ASC LIMIT 10")
        result.show()
        //result.write.mode("overwrite").csv("results/Bottom10ConfirmedCasesUS")
       
    }
    //Sharyar
    def top10CasesByDate(hiveCtx:HiveContext): Unit = {
        println("=== Top 10 total cases on (5/2/2021) by location ===")
        val result = hiveCtx.sql("SELECT location, date, Max(total_cases) AS Max_Total_Cases from covid1 WHERE date = '5/2/2021' AND location NOT IN ('High income', 'Upper middle income', 'Lower middle income') GROUP BY date, location ORDER BY Max_Total_Cases DESC LIMIT 10")
        result.show()
        //result.write.mode("overwrite").csv("results/top10CasesByDate")
    }
    //Sharyar
    def btm10CasesByDate(hiveCtx:HiveContext): Unit = {
        println("=== Bottom 10 total cases on (5/2/2021) by location ===")
        val result = hiveCtx.sql("SELECT location, date, Min(total_cases) AS Min_Total_Cases from covid1 WHERE date = '5/2/2021' AND total_cases >= 0 GROUP BY date, location ORDER BY Min_Total_Cases ASC LIMIT 10")
        result.show()
        //result.write.mode("overwrite").csv("results/Bottom10CasesByDate")
    }

}