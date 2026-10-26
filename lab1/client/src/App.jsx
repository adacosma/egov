import { useState } from "react";
import { TEATRU, TVA, REDUCERE_STUDENT, MAX_BILETE, SPECTACOLE } from "./data";
import "./App.css";

const API = "http://localhost:8080";

const INITIAL = {
  nume: "", prenume: "", cnp: "", email: "", telefon: "",
  spectacol: "", reprezentatie: "", categorie: "",
  nrBileteIntregi: 1, nrBileteStudenti: 0,
};

const round2 = (n) => Math.round(n * 100) / 100;
const esc = (s) => String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
const fmtData = (d) => new Date(d).toLocaleDateString("ro-RO");

function cnpValid(c) {
  return /^\d{13}$/.test(c);
}

export default function App() {
  const [form, setForm] = useState(INITIAL);
  const [touched, setTouched] = useState({});
  const [submitted, setSubmitted] = useState(false);
  const [sending, setSending] = useState(false);
  const [rezultat, setRezultat] = useState(null);

  const handle = (e) => setForm({ ...form, [e.target.name]: e.target.value });
  const changeSpectacol = (e) =>
      setForm({ ...form, spectacol: e.target.value, reprezentatie: "", categorie: "" });
  const blur = (e) => setTouched({ ...touched, [e.target.name]: true });

  const spec = SPECTACOLE[form.spectacol];
  const rep = spec && form.reprezentatie !== "" ? spec.reprezentatii[Number(form.reprezentatie)] : null;
  const pret = spec && form.categorie ? spec.preturi[form.categorie] : 0;

  const nIntregi = Number(form.nrBileteIntregi) || 0;
  const nStudenti = Number(form.nrBileteStudenti) || 0;
  const totalBilete = nIntregi + nStudenti;

  const subtotalIntregi = round2(nIntregi * pret);
  const subtotalStudenti = round2(nStudenti * pret * REDUCERE_STUDENT);
  const subtotal = round2(subtotalIntregi + subtotalStudenti);

  const reducere = subtotalStudenti;
  const total = subtotal;
  const baza = round2(total / (1 + TVA));
  const tva = round2(total - baza);

  const errors = {};
  if (!/^[\p{L}][\p{L}' -]+$/u.test(form.nume.trim())) errors.nume = "Introdu un nume valid";
  if (!/^[\p{L}][\p{L}' -]+$/u.test(form.prenume.trim())) errors.prenume = "Introdu un prenume valid";
  if (!cnpValid(form.cnp)) errors.cnp = "CNP invalid (13 cifre)";
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(form.email)) errors.email = "Email invalid";
  if (!/^(\+40|0)7\d{8}$/.test(form.telefon.replace(/\s/g, ""))) errors.telefon = "Telefon invalid (ex: 0722000000)";
  if (!form.spectacol) errors.spectacol = "Alege un spectacol";
  if (form.reprezentatie === "") errors.reprezentatie = "Alege data";
  if (!form.categorie) errors.categorie = "Alege categoria de loc";
  if (!(totalBilete >= 1 && totalBilete <= MAX_BILETE))
    errors.nrBilete = `Alege între 1 și ${MAX_BILETE} bilete în total`;

  const arata = (n) => ((submitted || touched[n]) ? errors[n] : undefined);

  const submit = async (e) => {
    e.preventDefault();
    setSubmitted(true);
    if (Object.keys(errors).length > 0) return;

    const xml = `<?xml version="1.0" encoding="UTF-8"?>
<comanda>
  <nume>${esc(form.nume.trim())}</nume>
  <prenume>${esc(form.prenume.trim())}</prenume>
  <cnp>${form.cnp}</cnp>
  <email>${esc(form.email.trim())}</email>
  <telefon>${esc(form.telefon.trim())}</telefon>
  <teatru>${esc(TEATRU.nume)}</teatru>
  <spectacol>${esc(form.spectacol)}</spectacol>
  <dataSpectacol>${rep.data}</dataSpectacol>
  <ora>${rep.ora}</ora>
  <categorieLoc>${esc(form.categorie)}</categorieLoc>
  <nrBileteIntregi>${nIntregi}</nrBileteIntregi>
  <nrBileteStudenti>${nStudenti}</nrBileteStudenti>
  <nrBileteTotal>${totalBilete}</nrBileteTotal>
  <pretUnitar>${pret}</pretUnitar>
  <subtotal>${subtotal.toFixed(2)}</subtotal>
  <reducere>${reducere.toFixed(2)}</reducere>
  <bazaImpozabila>${baza.toFixed(2)}</bazaImpozabila>
  <tva>${tva.toFixed(2)}</tva>
  <total>${total.toFixed(2)}</total>
  <iban>${TEATRU.iban}</iban>
</comanda>`;

    setSending(true);
    setRezultat(null);
    try {
      const res = await fetch(`${API}/api/payments`, {
        method: "POST",
        headers: { "Content-Type": "application/xml; charset=UTF-8" },
        body: xml,
      });
      if (!res.ok) {
        const body = await res.json().catch(() => null);
        throw new Error(body?.message || body?.detail || `Eroare ${res.status}`);
      }
      const id = await res.json();
      setRezultat({ ok: true, id });
    } catch (err) {
      setRezultat({ ok: false, msg: err.message });
    } finally {
      setSending(false);
    }
  };

  const reset = () => {
    setForm(INITIAL);
    setTouched({});
    setSubmitted(false);
    setRezultat(null);
  };

  if (rezultat?.ok) {
    return (
        <div className="page-wrapper">
          <div className="form-card success-card">
            <h2 className="success-title">Plată înregistrată!</h2>
            <p className="success-desc">
              Plata nr. <b>{rezultat.id}</b> a fost trimisă cu succes către {TEATRU.nume}.
            </p>
            <div className="btn-group">
              <a
                  className="submit-btn" style={{ textAlign: "center", textDecoration: "none", display: "block" }}
                  href={`${API}/api/payments/${rezultat.id}/ordin`}
                  target="_blank"
                  rel="noreferrer"
              >
                Descarcă ordinul de plată (PDF)
              </a>
              <button className="secondary-btn" onClick={reset}>
                Plată nouă
              </button>
            </div>
          </div>
        </div>
    );
  }

  return (
      <div className="page-wrapper">
        <div className="form-card">
          <div className="form-header">
            <h1>Bilete la Operetă</h1>
            <p>{TEATRU.nume}</p>
          </div>

          <form onSubmit={submit} noValidate>
            <div className="form-section">
              <div className="section-title">1. Date Cumpărător</div>

              <div className="row">
                <div className="col">
                  <div className="field-group">
                    <label>Nume</label>
                    <input type="text" name="nume" value={form.nume} onChange={handle} onBlur={blur} placeholder="" />
                    {arata("nume") && <div className="error-text">{arata("nume")}</div>}
                  </div>
                </div>
                <div className="col">
                  <div className="field-group">
                    <label>Prenume</label>
                    <input type="text" name="prenume" value={form.prenume} onChange={handle} onBlur={blur} placeholder="" />
                    {arata("prenume") && <div className="error-text">{arata("prenume")}</div>}
                  </div>
                </div>
              </div>

              <div className="field-group">
                <label>CNP</label>
                <input type="text" name="cnp" maxLength={13} value={form.cnp} onChange={handle} onBlur={blur} placeholder="" />
                {arata("cnp") && <div className="error-text">{arata("cnp")}</div>}
              </div>

              <div className="row">
                <div className="col">
                  <div className="field-group">
                    <label>Email</label>
                    <input type="email" name="email" value={form.email} onChange={handle} onBlur={blur} placeholder="" />
                    {arata("email") && <div className="error-text">{arata("email")}</div>}
                  </div>
                </div>
                <div className="col">
                  <div className="field-group">
                    <label>Telefon</label>
                    <input type="text" name="telefon" value={form.telefon} onChange={handle} onBlur={blur} placeholder="" />
                    {arata("telefon") && <div className="error-text">{arata("telefon")}</div>}
                  </div>
                </div>
              </div>
            </div>

            <div className="form-section">
              <div className="section-title">2. Detalii Spectacol</div>

              <div className="field-group">
                <label>Spectacol</label>
                <select name="spectacol" value={form.spectacol} onChange={changeSpectacol} onBlur={blur}>
                  <option value="">-- alege spectacolul --</option>
                  {Object.keys(SPECTACOLE).map((s) => <option key={s} value={s}>{s}</option>)}
                </select>
                {arata("spectacol") && <div className="error-text">{arata("spectacol")}</div>}
              </div>

              <div className="row">
                <div className="col">
                  <div className="field-group">
                    <label>Data și Ora</label>
                    <select name="reprezentatie" value={form.reprezentatie} onChange={handle} onBlur={blur} disabled={!spec}>
                      <option value="">-- alege data --</option>
                      {spec && spec.reprezentatii.map((r, i) => (
                          <option key={i} value={i}>{fmtData(r.data)} · {r.ora}</option>
                      ))}
                    </select>
                    {arata("reprezentatie") && <div className="error-text">{arata("reprezentatie")}</div>}
                  </div>
                </div>
                <div className="col">
                  <div className="field-group">
                    <label>Categorie Loc</label>
                    <select name="categorie" value={form.categorie} onChange={handle} onBlur={blur} disabled={!spec}>
                      <option value="">-- alege categoria --</option>
                      {spec && Object.entries(spec.preturi).map(([c, p]) => (
                          <option key={c} value={c}>{c} ({p} RON)</option>
                      ))}
                    </select>
                    {arata("categorie") && <div className="error-text">{arata("categorie")}</div>}
                  </div>
                </div>
              </div>

              <div className="row">
                <div className="col">
                  <div className="field-group">
                    <label>Bilete Întregi</label>
                    <input type="number" min="0" max={MAX_BILETE} name="nrBileteIntregi" value={form.nrBileteIntregi} onChange={handle} onBlur={blur} />
                  </div>
                </div>
                <div className="col">
                  <div className="field-group">
                    <label>Bilete Studenți (-50%)</label>
                    <input type="number" min="0" max={MAX_BILETE} name="nrBileteStudenti" value={form.nrBileteStudenti} onChange={handle} onBlur={blur} />
                  </div>
                </div>
              </div>
              <p className="student-note" style={{ fontSize: "12px", color: "#6b7280", marginTop: "4px" }}>
                * Legitimațiile pentru biletele de student se vor prezenta la intrarea în sală.
              </p>
              {arata("nrBilete") && <div className="error-text">{arata("nrBilete")}</div>}
            </div>

            <div className="summary-box">
              <div className="summary-row">
                <span>Subtotal:</span>
                <b>{subtotal.toFixed(2)} RON</b>
              </div>
              {reducere > 0 && (
                  <div className="summary-row" style={{ color: "#16a34a" }}>
                    <span>Reducere studenți (50%):</span>
                    <b>-{reducere.toFixed(2)} RON</b>
                  </div>
              )}
              <div className="summary-row">
                <span>Bază impozabilă:</span>
                <b>{baza.toFixed(2)} RON</b>
              </div>
              <div className="summary-row">
                <span>TVA (21%):</span>
                <b>{tva.toFixed(2)} RON</b>
              </div>
              <div className="summary-total">
                <span>TOTAL DE PLATĂ:</span>
                <span>{total.toFixed(2)} RON</span>
              </div>
            </div>

            {rezultat && !rezultat.ok && (
                <div className="error-banner">
                  Eroare la trimitere: {rezultat.msg}
                </div>
            )}

            <button className="submit-btn" type="submit" disabled={sending}>
              {sending ? "Se trimite către server..." : "Plătește"}
            </button>
          </form>
        </div>
      </div>
  );
}