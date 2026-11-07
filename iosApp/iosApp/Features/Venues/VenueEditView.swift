import SwiftUI
import shared

@MainActor
final class VenueEditModel: ObservableObject {
    private let viewModel: VenueDetailViewModel
    private let lifetime: ViewModelLifetime
    @Published var venue: VenueDto?

    init(venueId: String) {
        viewModel = KoinIosKt.venueDetailViewModel(venueId: venueId)
        lifetime = ViewModelLifetime(viewModel)
        venue = viewModel.venue.value as? VenueDto
        lifetime.observe(viewModel.venue, as: VenueDto.self) { [weak self] in
            self?.venue = $0
        }
    }

    func save(_ form: VenueForm) {
        viewModel.save(
            name: form.name,
            location: form.location,
            code: form.code,
            contactPerson: form.contactPerson,
            contactPhone: form.contactPhone,
            isHeadCountEnabled: form.headCount,
            isLostAndFoundEnabled: form.lostFound,
            isIncidentReportingEnabled: form.incidents
        )
    }
}

struct VenueForm {
    var name = ""
    var location = ""
    var code = ""
    var contactPerson = ""
    var contactPhone = ""
    var headCount = true
    var lostFound = false
    var incidents = false

    init() {
    }

    init(_ venue: VenueDto) {
        name = venue.name
        location = venue.location
        code = venue.code
        contactPerson = venue.contactPerson
        contactPhone = venue.contactPhone
        headCount = venue.isHeadCountEnabled
        lostFound = venue.isLostAndFoundEnabled
        incidents = venue.isIncidentReportingEnabled
    }
}

struct VenueEditView: View {
    @StateObject private var model: VenueEditModel
    @Environment(\.dismiss) private var dismiss
    @State private var form = VenueForm()
    @State private var loadedId: String?

    init(venueId: String) {
        _model = StateObject(wrappedValue: VenueEditModel(venueId: venueId))
    }

    var body: some View {
        Group {
            if model.venue == nil {
                LoadingView()
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        OutlinedField(label: "Name", text: $form.name)
                        OutlinedField(label: "Location", text: $form.location)
                        OutlinedField(label: "Code", text: $form.code)
                            .textInputAutocapitalization(.characters)
                            .autocorrectionDisabled()
                        OutlinedField(label: "Contact person", text: $form.contactPerson)
                            .textContentType(.name)
                        OutlinedField(label: "Contact phone", text: $form.contactPhone, keyboard: .phonePad)
                            .textContentType(.telephoneNumber)
                        Text("Features").font(.subheadline.weight(.medium)).foregroundStyle(AppColor.text)
                        Toggle("Head count", isOn: $form.headCount)
                        Toggle("Lost & found", isOn: $form.lostFound)
                        Toggle("Incident reporting", isOn: $form.incidents)
                        WideButton(title: "Save") {
                            model.save(form)
                            dismiss()
                        }
                        .disabled(form.name.trimmingCharacters(in: .whitespaces).isEmpty)
                    }
                    .foregroundStyle(AppColor.text)
                    .padding(16)
                }
            }
        }
        .appBackground()
        .appNavigationTitle("Edit Venue")
        .onReceive(model.$venue) { venue in
            // Seed the form once; later emissions shouldn't clobber in-progress edits.
            guard let venue, loadedId != venue.id else {
                return
            }
            form = VenueForm(venue)
            loadedId = venue.id
        }
    }
}
