using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Text;

namespace CSharp
{
    class Program
    {
        static void Main(string[] args)
        {
          TextWriter writer = new  StreamWriter("CSharpTestdata.txt",false);

           Random r = new Random();

           for(int x=0;x<5000;x++)
           {
              byte[] keydata = new byte[16];
              r.NextBytes(keydata);
              uint rollingStartIntervalNumber = Convert.ToUInt32( r.Next());
              uint rollingperiod = Convert.ToUInt32(r.Next());
              int transmissionRiskLevel = r.Next();
              List<string> visitedCountries = new List<string>();
              var cultures = System.Globalization.CultureInfo.GetCultures(CultureTypes.SpecificCultures);
              for(int c = 0;c<10;c++)
              {
                    var country= r.Next(cultures.Length);
                    var regionInfo = new RegionInfo(cultures[country].LCID);
                    visitedCountries.Add(regionInfo.TwoLetterISORegionName);
              }

              int daysOnsetOfSymptoms = r.Next(-50,50);

              List<string> line = new List<string>();
              
              line.Add( "["+string.Join(",",keydata)+"]");
              line.Add( System.Convert.ToBase64String(keydata));
              line.Add( "["+string.Join(",",BitConverter.GetBytes(rollingStartIntervalNumber))+"]");
              line.Add( System.Convert.ToBase64String(BitConverter.GetBytes(rollingStartIntervalNumber)));
              line.Add( "["+string.Join(",",BitConverter.GetBytes(rollingperiod))+"]");
              line.Add( System.Convert.ToBase64String(BitConverter.GetBytes(rollingperiod)));
              line.Add( "["+string.Join(",",BitConverter.GetBytes(transmissionRiskLevel))+"]");        
              line.Add( System.Convert.ToBase64String(BitConverter.GetBytes(transmissionRiskLevel)));
              line.Add( "["+string.Join(",",visitedCountries)+"]");
              line.Add( System.Convert.ToBase64String(Encoding.ASCII.GetBytes(string.Join(",",visitedCountries))));
              line.Add( "["+string.Join(",","DE")+"]");
              line.Add( System.Convert.ToBase64String(Encoding.ASCII.GetBytes("DE")));  
              line.Add( "["+string.Join(",",BitConverter.GetBytes(1).Reverse().ToArray())+"]");
              line.Add( System.Convert.ToBase64String(BitConverter.GetBytes(1).Reverse().ToArray())); 
              line.Add( "["+string.Join(",",BitConverter.GetBytes(daysOnsetOfSymptoms))+"]");
              line.Add( System.Convert.ToBase64String(BitConverter.GetBytes(daysOnsetOfSymptoms))); 
              line.Add(System.Convert.ToBase64String(Encoding.ASCII.GetBytes(
                      string.Join(".", System.Convert.ToBase64String(keydata),
                                        System.Convert.ToBase64String(BitConverter.GetBytes(rollingStartIntervalNumber)),
                                        System.Convert.ToBase64String(BitConverter.GetBytes(rollingperiod)),
                                        System.Convert.ToBase64String(BitConverter.GetBytes(transmissionRiskLevel)),
                                        System.Convert.ToBase64String(Encoding.ASCII.GetBytes(string.Join(",",visitedCountries))),
                                        System.Convert.ToBase64String(Encoding.ASCII.GetBytes(string.Join(",","DE"))),
                                        System.Convert.ToBase64String(BitConverter.GetBytes(1).Reverse().ToArray()),
                                        System.Convert.ToBase64String(BitConverter.GetBytes(daysOnsetOfSymptoms))
                                  )+".")));

             writer.WriteLine(string.Join("|",line));
              
    }

    writer.Flush();
    writer.Close();
}}}
