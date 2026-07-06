"use client";

import { useState } from "react";

type QuizQuestion = {
  id: string;
  question: string;
  options: string[];
};

export type QuizDetails = {
  videoId: string;
  model: string;
  generatedAt: string;
  questions: QuizQuestion[];
};

type GradeResult = {
  questionId: string;
  question: string;
  selectedOptionIndex: number | null;
  correctOptionIndex: number;
  selectedAnswer: string | null;
  correctAnswer: string;
  correct: boolean;
  explanation: string;
};

type GradeResponse = {
  videoId: string;
  score: number;
  total: number;
  percentage: number;
  results: GradeResult[];
};

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

export function VideoQuizPanel({
  videoId,
  disabled,
  initialQuiz,
}: {
  videoId: string;
  disabled: boolean;
  initialQuiz: QuizDetails | null;
}) {
  const [quiz, setQuiz] = useState<QuizDetails | null>(initialQuiz);
  const [answers, setAnswers] = useState<Record<string, number>>({});
  const [grade, setGrade] = useState<GradeResponse | null>(null);
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");
  const [isGenerating, setIsGenerating] = useState(false);
  const [isGrading, setIsGrading] = useState(false);

  async function generateQuiz() {
    setError("");
    setStatus("Generating true/false quiz with OpenAI...");
    setIsGenerating(true);
    setGrade(null);

    try {
      const response = await fetch(
        `${apiBaseUrl}/api/content/${encodeURIComponent(videoId)}/quiz`,
        { method: "POST" }
      );
      const body = await response.json();

      if (!response.ok) {
        throw new Error(body?.message || body?.error || "Quiz generation failed.");
      }

      setQuiz(body as QuizDetails);
      setAnswers({});
      setStatus(`Quiz generated with ${body.model}.`);
    } catch (exception) {
      setError(
        exception instanceof Error
          ? exception.message
          : "Quiz generation failed."
      );
      setStatus("");
    } finally {
      setIsGenerating(false);
    }
  }

  async function gradeQuiz() {
    if (!quiz) {
      setError("Generate a quiz first.");
      return;
    }

    setError("");
    setStatus("Grading your answers...");
    setIsGrading(true);

    try {
      const response = await fetch(
        `${apiBaseUrl}/api/content/${encodeURIComponent(videoId)}/quiz/grade`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            answers: quiz.questions.map((question) => ({
              questionId: question.id,
              selectedOptionIndex: answers[question.id],
            })),
          }),
        }
      );
      const body = await response.json();

      if (!response.ok) {
        throw new Error(body?.message || body?.error || "Quiz grading failed.");
      }

      setGrade(body as GradeResponse);
      setStatus("Quiz graded.");
    } catch (exception) {
      setError(
        exception instanceof Error ? exception.message : "Quiz grading failed."
      );
      setStatus("");
    } finally {
      setIsGrading(false);
    }
  }

  const answeredCount = quiz
    ? quiz.questions.filter((question) => answers[question.id] !== undefined).length
    : 0;
  const allAnswered = quiz ? answeredCount === quiz.questions.length : false;

  return (
    <section className="panel">
      <div className="stack">
        <p className="eyebrow">Quiz</p>
        <h2 className="section-title">Test yourself with true/false questions.</h2>
        <p className="body-copy">
          OpenAI generates the quiz from the stored transcript. Your answers are
          graded by the backend against the saved answer key.
        </p>
      </div>

      <button
        className={`button ${disabled ? "button-muted" : "button-dark"}`}
        type="button"
        onClick={generateQuiz}
        disabled={disabled || isGenerating || isGrading}
      >
        {isGenerating
          ? "Generating quiz..."
          : quiz
            ? "Regenerate quiz"
            : "Generate true/false quiz"}
      </button>

      {disabled ? (
        <p className="status-text">Fetch a transcript before generating a quiz.</p>
      ) : null}
      {status ? <p className="status-text">{status}</p> : null}
      {error ? <p className="status-warning">{error}</p> : null}

      {quiz ? (
        <div className="quiz-list">
          <div className="meta-row">
            <span>{quiz.questions.length} questions</span>
            <span>{answeredCount} answered</span>
          </div>

          {quiz.questions.map((question, questionIndex) => {
            const result = grade?.results.find(
              (item) => item.questionId === question.id
            );

            return (
              <div className="quiz-question" key={question.id}>
                <p className="metric-value">
                  {questionIndex + 1}. {question.question}
                </p>

                <div className="quiz-options">
                  {question.options.map((option, optionIndex) => {
                    const selected = answers[question.id] === optionIndex;
                    const correct = result?.correctOptionIndex === optionIndex;
                    const wrongSelection =
                      result && selected && !result.correct;

                    return (
                      <label
                        className={`quiz-option ${
                          selected ? "quiz-option-selected" : ""
                        } ${correct ? "quiz-option-correct" : ""} ${
                          wrongSelection ? "quiz-option-wrong" : ""
                        }`}
                        key={option}
                      >
                        <input
                          type="radio"
                          name={question.id}
                          value={optionIndex}
                          checked={selected}
                          disabled={Boolean(grade)}
                          onChange={() =>
                            setAnswers((current) => ({
                              ...current,
                              [question.id]: optionIndex,
                            }))
                          }
                        />
                        <span>{option}</span>
                      </label>
                    );
                  })}
                </div>

                {result ? (
                  <p
                    className={result.correct ? "status-text" : "status-warning"}
                  >
                    {result.correct ? "Correct." : "Incorrect."}{" "}
                    {result.explanation}
                  </p>
                ) : null}
              </div>
            );
          })}

          {!grade ? (
            <button
              className={`button ${
                allAnswered ? "button-accent" : "button-muted"
              }`}
              type="button"
              onClick={gradeQuiz}
              disabled={!allAnswered || isGrading}
            >
              {isGrading ? "Grading..." : "Grade quiz"}
            </button>
          ) : (
            <div className="quiz-score">
              <p className="eyebrow">Result</p>
              <h3 className="section-title">
                {grade.score}/{grade.total} - {grade.percentage}%
              </h3>
            </div>
          )}
        </div>
      ) : null}
    </section>
  );
}
