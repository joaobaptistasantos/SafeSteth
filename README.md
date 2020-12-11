# StethForCOVID

The actual pandemic caused by SARS-CoV-2, or COVID-19, forces ourselves to adapt to new ways of interaction and social communicate. Because of this pandemic we started to use masks on streets and closed spaces, we have to maintain social distancing, and many other restrictions to keep us safe. Besides that, but still caused by COVID-19 pandemic, the auscultation practise become compromised. In order to doctors ensure that they are fully protected and safe dealing with their patients, they need to wear protective and certified clothing. This clothing, to meet all the certification standards, needs to be completelly closed what makes it impossible to have the stethoscope on the doctor's ear.

StethStream is the solution to that problem since it makes possible to a doctor, just having an Android mobile phone and a Littmann Model 3200 with him, to auscultate his patient with his protective clothing. The software is an Android mobile application that handles the bluetooth communication between the stethoscope and the headphones. This bluetooth communication have the contraint of being a classical Bluetooth 2.0 communication since this is the supported communication on the Littmann Model 3200.

## Instructions of usage

To be capable of use the **StethForCOVID** application is required to download and install the application from the [Google Play Store](https://play.google.com/store/apps/details?id=com.uc.health.stethstream).

### Automatic Connection

In order to connect automatically is just needed that the user starts the application and manages the Littmann by following the next instructions:

1. Run the application on your device
2. Turn on the Littmann Model 3200
3. Click 'M' on the stethoscope
4. With the 'Connect' option selected, click 'M' again on the stethoscope
5. Wait unitl the connected menu appears and click 'M' on the stethoscope in order to start, or stop, streaming

### Mannually Connection

If the user wants to connect manually, follow the next instructions:

1. Run the application on your device
2. Turn on the Littmann Model 3200
3. Click on 'Add Stethoscope' button on the application graphical interface
4. Wait until the bluetooth name of the stethoscope shows up
5. Click on the 'Pair' button of the stethoscope's bluetooth device
6. Click 'M' on the stethoscope
7. With the 'Connect' option selected, click 'M' again on the stethoscope
8. Wait unitl the connected menu appears and click 'M' on the stethoscope in order to start, or stop, streaming

### Notes

If it is the first usage it's required to pair the stethoscope on the Bluetooth settings on your's Android device. 

## Requirements

* Android 7.0 (API Level 24) or higher
* [Littmann Electronic Stethoscope Model 3200](https://www.littmann.com/3M/en_US/littmann-stethoscopes/products/~/3M-Littmann-Electronic-Stethoscope-Model-3200/?N=5932256+8711017+3293188392&rt=rud)

We recommend the usage of bluetooth headphones for a more practical usage.

## Built With

* [JAVA](https://www.java.com) - Programming language used
* [Android Studio](https://developer.android.com/studio) 4.1 - IDE used

## Versioning

For the versions available, see the [tags on this repository](https://github.com/joaobaptistasantos/StethForCOVID/tags). 

## Future Work

- [ ] Reduce the existing latency on auscultation using Android NDK
- [ ] Make it possible to record audio tracks, during a short period of time, in order to students or junior doctors 

We will constantly trying to fix errors that might happen and for that we hope all users report them to us.

## Authors

* **João Santos** - Follow my work on [Github](https://github.com/joaobaptistasantos) or [LinkedIn](https://www.linkedin.com/in/joão-santos-1a3649143/)

With the support of

* [Pr. Paulo de Carvalho](https://www.cisuc.uc.pt/en/people/paulo-carvalho) and [Pr. Dr. Henrique Madeira](https://www.cisuc.uc.pt/en/people/henrique-madeira) from [CISUC](https://www.cisuc.uc.pt) - [University of Coimbra](https://www.uc.pt)
* [Dr. Carlos Robalo Cordeiro](https://www.uc.pt/fmuc/pessoas/docentes/CarlosCordeiro) e [Dr. Tiago Alfaro](https://www.uc.pt/fmuc/pessoas/docentes/TiagoAlfaro) from [CHUC](https://www.chuc.min-saude.pt)

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details