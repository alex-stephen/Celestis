package com.alexstephen.celestis80085.legal

enum class LegalDocument(
    val title: String,
    val body: String
) {
    PrivacyPolicy(
        title = "Privacy Policy",
        body = """
            Last updated: May 14, 2026

            This Privacy Policy explains how Celestis handles information when you use the Celestis mobile application, including the app, widgets, notifications, and related app functionality.

            Celestis is designed as a space image viewer for NASA Astronomy Picture of the Day content. The app does not sell your personal information, does not track you across apps or websites, and does not use your data for third-party advertising.

            Interpretation and Definitions

            For this Privacy Policy:

            Application means Celestis, the mobile application provided by the developer.
            Company, We, Us, or Our means the developer of Celestis.
            Device means any phone, tablet, or other device that can run the Application.
            Personal Data means information that relates to an identified or identifiable individual.
            Service means the Application.
            Service Provider means a third party that processes data to help provide app functionality.
            Usage Data means technical or diagnostic data generated when the Service is used.
            You means the person using the Service.

            Information We Collect

            Celestis does not require an account and does not ask you to provide your name, address, phone number, or payment details.

            The app may process app interaction data, diagnostic and crash data, push notification identifiers when notifications are enabled, and local app content such as cached APOD metadata, cached images, favourites, and widget data stored on your device.

            How We Use Information

            Celestis uses information only to provide, maintain, and improve the Service. This includes displaying NASA Astronomy Picture of the Day content, caching APOD content, delivering daily notifications when enabled, registering or refreshing push notification tokens with the Celestis backend, diagnosing crashes, and respecting low-data or constrained network modes reported by the operating system.

            Local Storage and Cache

            Celestis stores APOD metadata, image cache data, favourites, and widget data locally on your Device. Favourites are kept when you use Clear cache. Non-favourite cached APOD records may be removed when you clear cache or when the app prunes older cached content.

            Notifications

            If you allow notifications, Celestis may use Firebase Cloud Messaging, Apple Push Notification service, and local notification scheduling to deliver daily APOD notifications. Notification permission can be changed at any time in your system settings.

            Service Providers

            Celestis may use NASA services or a Celestis backend for APOD content, Firebase Cloud Messaging for push notification delivery, Firebase Crashlytics for crash reporting and diagnostics, and Apple and Google platform services for app distribution, notifications, widgets, and operating system functionality.

            Sharing of Information

            We do not sell your Personal Data. We may share limited information with Service Providers needed to operate app functionality, to comply with legal obligations, to protect rights and safety, in connection with a business transfer, or with your consent, such as when you choose to share APOD content through the system share sheet.

            Retention

            Locally cached APOD content remains on your Device until it is cleared by you, pruned by the app, or removed by the operating system. Diagnostic and notification-related data retained by Service Providers is kept according to their retention practices and only as long as reasonably necessary for app functionality, reliability, legal compliance, or security.

            Delete Your Data

            You can remove non-favourite cached APOD content using Clear cache in Settings. You can delete the app to remove app data stored by the Application on your Device, subject to normal operating system behaviour and any cloud/device backups you control.

            To request help with privacy questions, contact us at support.celestis@gmail.com.

            Children's Privacy

            Celestis is not directed to children under 13. We do not knowingly collect Personal Data from children under 13. If you believe a child has provided Personal Data through the Service, contact us so we can take appropriate action.

            International Processing

            Your information may be processed in the United States or other locations where Service Providers operate. Data protection laws may differ from those in your jurisdiction.

            Security

            We use reasonable measures to protect information handled by the Service. No method of electronic transmission or storage is completely secure, so absolute security cannot be guaranteed.

            Links and Third-Party Content

            Celestis may display or link to NASA content, media, websites, or other third-party services. We are not responsible for third-party privacy practices. Review third-party privacy policies before using their services.

            Changes to This Privacy Policy

            We may update this Privacy Policy from time to time. Changes are effective when posted in the Application or otherwise made available. The Last updated date will reflect the latest revision.

            Contact Us

            For questions about this Privacy Policy, contact:

            support.celestis@gmail.com
        """.trimIndent()
    ),
    TermsOfUse(
        title = "Terms of Use",
        body = """
            Last updated: May 14, 2026

            Please read these Terms of Use carefully before using Celestis.

            By using Celestis, you agree to these Terms. If you do not agree, do not use the Application.

            Interpretation and Definitions

            For these Terms:

            Application means Celestis, the mobile application provided by the developer.
            Application Store means the Apple App Store, Google Play Store, or another digital distribution service where the Application is made available.
            Company, We, Us, or Our means the developer of Celestis.
            Device means any phone, tablet, or other device that can run the Application.
            Service means the Application, including app screens, widgets, notifications, and related functionality.
            Terms means these Terms of Use.
            Third-party Service means any content, data, website, product, or service not owned or controlled by Us.
            You means the person using the Service.

            Acknowledgment

            These Terms govern your use of Celestis. Your access to and use of the Service is conditioned on your acceptance of and compliance with these Terms and the Privacy Policy.

            Celestis displays astronomy content, including NASA Astronomy Picture of the Day content and related metadata. The Service may cache content locally, show widgets, send notifications, and provide sharing features.

            Eligibility

            You must be legally able to use the Service in your jurisdiction. If you use the Service on behalf of another person or entity, you represent that you have authority to accept these Terms on their behalf.

            Use of the Service

            You agree to use Celestis only for lawful, personal, and appropriate purposes. You agree not to misuse the Service, interfere with its operation, attempt unauthorized access to systems, or use the Service in a way that violates applicable laws or third-party rights.

            NASA and Third-Party Content

            Celestis may display NASA content, third-party media, copyright notices, links, explanations, and other metadata. Some APOD content may be copyrighted by third parties. You are responsible for respecting all copyright notices, credits, licenses, and third-party terms that apply to content shown in the Service.

            Celestis does not claim ownership of NASA or third-party content displayed through the Service.

            Links to Other Websites

            The Service may contain links to third-party websites or services that are not owned or controlled by Us. We have no control over and assume no responsibility for third-party content, privacy policies, or practices. You should review the terms and privacy policies of third-party websites or services you visit.

            Notifications and Background Activity

            Celestis may offer daily notifications, widgets, background sync, and cached content features. These features depend on operating system permissions, network availability, Service Providers, and device settings. You can manage notifications through system settings.

            Purchases and Support

            Celestis may add optional support, donation, subscription, or purchase features in the future. Any such features will be governed by the applicable Application Store rules and any additional terms shown at the time of purchase.

            Termination

            We may suspend or terminate access to the Service immediately, without prior notice or liability, if you breach these Terms or if continued access would create legal, security, or operational risk.

            Upon termination, your right to use the Service will cease immediately.

            AS IS and AS AVAILABLE Disclaimer

            The Service is provided to you AS IS and AS AVAILABLE, with all faults and defects and without warranty of any kind. To the maximum extent permitted by law, We disclaim all warranties, whether express, implied, statutory, or otherwise, including implied warranties of merchantability, fitness for a particular purpose, title, non-infringement, availability, accuracy, and reliability.

            We do not warrant that the Service will be uninterrupted, error-free, secure, current, compatible with every device, or that defects will be corrected.

            Limitation of Liability

            To the maximum extent permitted by applicable law, We will not be liable for any special, incidental, indirect, consequential, exemplary, or punitive damages, including loss of profits, loss of data, business interruption, personal injury, loss of privacy, or damages related to use of or inability to use the Service.

            Some jurisdictions do not allow certain limitations of liability, so some limitations may not apply to you. In those jurisdictions, liability will be limited to the greatest extent permitted by law.

            Governing Law

            These Terms are governed by the laws of the United States, excluding conflict of law rules, unless mandatory laws in your place of residence require otherwise.

            Dispute Resolution

            If you have a concern or dispute about the Service, you agree to first try to resolve it informally by contacting Us.

            European Union Users

            If you are a European Union consumer, you will benefit from any mandatory provisions of the law of the country in which you reside.

            United States Legal Compliance

            You represent and warrant that you are not located in a country subject to a United States government embargo or designated by the United States government as a terrorist supporting country, and that you are not listed on any United States government list of prohibited or restricted parties.

            Severability

            If any provision of these Terms is held to be unenforceable or invalid, that provision will be interpreted to accomplish its objectives to the greatest extent possible under applicable law, and the remaining provisions will remain in effect.

            Waiver

            Failure to enforce any right or provision of these Terms will not be considered a waiver of those rights. A waiver of one breach does not constitute a waiver of any later breach.

            Changes to These Terms

            We may update these Terms from time to time. Changes are effective when posted in the Application or otherwise made available. The Last updated date will reflect the latest revision. By continuing to use the Service after changes become effective, you agree to the revised Terms.

            Contact Us

            For questions about these Terms, contact:

            support.celestis@gmail.com
        """.trimIndent()
    )
}
