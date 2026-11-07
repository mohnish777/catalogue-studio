package com.cataloguestudio.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.cataloguestudio.R
import com.cataloguestudio.presentation.EnquiryState
import com.cataloguestudio.presentation.ProductUi
import com.cataloguestudio.presentation.UiText

private fun enquiryPreviewState() = EnquiryState(
    isLoading = false,
    product = ProductUi(
        id = 9,
        title = "A considered everyday essential with a longer product title",
        description = "A product description.",
        priceLabel = "$64.99",
        category = "electronics",
        categoryLabel = UiText.StringResource(R.string.category_electronics),
        imageUrl = "preview://unused",
        ratingLabel = "4.7",
        reviewCount = 203,
    ),
    name = "Alex Rivera",
    email = "alex@example.test",
    message = "Could you share more information about this product?",
)

@Preview(name = "Enquiry form", widthDp = 390, heightDp = 1100)
@Preview(name = "Narrow enquiry form", widthDp = 320, heightDp = 1250)
@Preview(name = "Larger enquiry text", widthDp = 390, heightDp = 1800, fontScale = 1.5f)
@Composable
private fun EnquiryFormPreview() {
    StudioTheme { EnquiryScreen(enquiryPreviewState(), onAction = {}) }
}

@Preview(name = "Invalid fields", widthDp = 390, heightDp = 1250)
@Composable
private fun EnquiryInvalidPreview() {
    StudioTheme {
        EnquiryScreen(enquiryPreviewState().copy(
            name = "", email = "alex@", showValidation = true,
            nameError = UiText.StringResource(R.string.error_name_required),
            emailError = UiText.StringResource(R.string.error_email_invalid),
        ), onAction = {})
    }
}

@Preview(name = "Sending", widthDp = 390, heightDp = 1200)
@Composable
private fun EnquirySendingPreview() {
    StudioTheme { EnquiryScreen(enquiryPreviewState().copy(isSending = true), onAction = {}) }
}

@Preview(name = "Failed send retains form", widthDp = 390, heightDp = 1300)
@Composable
private fun EnquiryFailedPreview() {
    StudioTheme {
        EnquiryScreen(enquiryPreviewState().copy(sendError = UiText.StringResource(R.string.error_server)), onAction = {})
    }
}

@Preview(name = "Enquiry received", widthDp = 390, heightDp = 850)
@Composable
private fun EnquirySuccessPreview() {
    StudioTheme { EnquiryScreen(EnquiryState(isLoading = false, isSent = true), onAction = {}) }
}
